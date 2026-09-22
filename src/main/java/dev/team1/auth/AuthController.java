package dev.team1.auth;

import dev.team1.auth.dtos.CredentialsDTO;
import dev.team1.mappers.UserMapper;
import dev.team1.security.JwtAuthentificationDTO;
import dev.team1.security.JwtService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.users.dtos.UserResponseDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RestController 
@RequestMapping(path = "${api-endpoint}/auth")
@RequiredArgsConstructor 
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @GetMapping("login")
    public ResponseEntity<UserResponseDTO> loginHandler(@RequestBody @Valid CredentialsDTO credentials, HttpServletResponse response) {
        UserResponseDTO userDto = authService.login(credentials);
        
        JwtAuthentificationDTO authDTO = jwtService.generateAuthToken(userDto.email(), UserMapper.rolesToString(userDto.roles()));

        Cookie cookieAccess = generateCookie("access_token", authDTO.token());
        Cookie cookieRefresh = generateCookie("refresh_token", authDTO.refreshToken());
        response.addCookie(cookieAccess);
        response.addCookie(cookieRefresh);
        
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("logout")
    public ResponseEntity<Void> logoutHandler(HttpServletResponse response) {
        
        Cookie cookieAccess = generateCookie("access_token", "");
        Cookie cookieRefresh = generateCookie("refresh_token", "");
        response.addCookie(cookieAccess);
        response.addCookie(cookieRefresh);
        
        return ResponseEntity.noContent().build();
    }

    private Cookie generateCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(
            (int) Duration.ofMinutes(15).toSeconds()
        );
        cookie.setPath("/");
        return cookie;
    }

}
