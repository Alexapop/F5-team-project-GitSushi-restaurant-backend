package dev.team1.auth;

import java.util.List;

import javax.security.sasl.AuthenticationException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import dev.team1.auth.dtos.CredentialsDTO;
import dev.team1.mappers.UserMapper;
import dev.team1.security.JwtService;
import dev.team1.security.dtos.JwtAuthenticationDTO;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;
import dev.team1.users.dtos.UserResponseDTO;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public UserResponseDTO login(CredentialsDTO credentials) {
        
        UserEntity user = userRepository.findByEmail(credentials.email())
            .orElseThrow(() -> new BadCredentialsException("User doesn't exist."));

        // TODO check password correctly
        if (!credentials.password().equals(user.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        } 

        return UserMapper.toDTO(user);
    }

    public JwtAuthenticationDTO getAuth(String email, List<String> roles) {
        return jwtService.generateAuthToken(email, UserMapper.rolesToString(roles));
    }

    public JwtAuthenticationDTO updateAuth(String oldRefreshToken) {
        if (oldRefreshToken == null) throw new JwtException("Invalid refresh token: token doesn't exist");
        
        jwtService.validateJwtToken(oldRefreshToken);
            
        String tokenEmail = jwtService.getEmailFromToken(oldRefreshToken);
        List<String> tokenRoles = jwtService.getRolesFromToken(oldRefreshToken);

        UserEntity user = userRepository.findByEmail(tokenEmail).orElse(null);
        if (user == null) throw new JwtException("Invalid refresh token: user with email doesn't exist");
        
        UserResponseDTO userDTO = UserMapper.toDTO(user);
        if (tokenRoles.size() != userDTO.roles().size() || tokenRoles.get(0).equals(userDTO.roles().get(0))) {
            throw new JwtException("Invalid refresh token: roles mismatch");
        }

        return jwtService.refreshBaseToken(tokenEmail, UserMapper.rolesToString(tokenRoles), oldRefreshToken);
    }

}
