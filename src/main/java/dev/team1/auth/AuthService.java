package dev.team1.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import dev.team1.auth.dtos.CredentialsDTO;
import dev.team1.mappers.UserMapper;
import dev.team1.users.UserEntity;
import dev.team1.users.UserRepository;
import dev.team1.users.dtos.UserResponseDTO;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthService {

    private final UserRepository userRepository;

    public UserResponseDTO login(CredentialsDTO credentials) {
        UserEntity user = userRepository.findByEmail(credentials.email())
            .orElseThrow(() -> new BadCredentialsException("User doesn't exist."));

        // TODO check password correctly
        if (credentials.password() != user.getPassword()) {
            throw new BadCredentialsException("Wrong password");
        } 

        return UserMapper.toDTO(user);
    }

}
