package dev.team1.auth;

import dev.team1.auth.dtos.CredentialsDTO;
import dev.team1.security.dtos.JwtAuthenticationDTO;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.users.dtos.UserResponseDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController 
@RequestMapping(path = "${api-endpoint}/auth")
@RequiredArgsConstructor 
public class AuthController {

    private final AuthService authService;
    
    @Value("/${api-endpoint}/auth/refresh")
    private String refreshPath;

    @Value("${cookie-same-site}")
    private String sameSite;

    @PostMapping("login")
    public ResponseEntity<UserResponseDTO> loginHandler(@RequestBody @Valid CredentialsDTO credentials, HttpServletResponse response) {
        
        UserResponseDTO userDto = authService.login(credentials);
        
        JwtAuthenticationDTO authDTO = authService.getAuth(userDto.email(), userDto.roles());
        
        Cookie cookieAccess = generateCookie("access_token", authDTO.token(), "/");
        Cookie cookieRefresh = generateCookie("refresh_token", authDTO.refreshToken(), refreshPath);
        response.addCookie(cookieAccess);
        response.addCookie(cookieRefresh);
        
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("logout")
    public ResponseEntity<Void> logoutHandler(HttpServletResponse response) {
        
        Cookie cookieAccess = generateCookie("access_token", "", "/");
        Cookie cookieRefresh = generateCookie("refresh_token", "", refreshPath);
        response.addCookie(cookieAccess);
        response.addCookie(cookieRefresh);
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("refresh")
    public ResponseEntity<Void> refreshHandler(
        @CookieValue(name = "refresh_token", required = false) String refreshToken, 
        HttpServletResponse response
    ) {
        JwtAuthenticationDTO authDto = authService.updateAuth(refreshToken);

        Cookie cookieAccess = generateCookie("access_token", authDto.token(), "/");
        Cookie cookieRefresh = generateCookie("refresh_token", authDto.refreshToken(), refreshPath);
        response.addCookie(cookieAccess);
        response.addCookie(cookieRefresh);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("me")
    public ResponseEntity<UserResponseDTO> getMeHandler(@AuthenticationPrincipal CustomUserDetails userPrincipal) {
        return ResponseEntity.ok(authService.getMe(userPrincipal.getUsername()));
    }
    

    private Cookie generateCookie(String key, String value, String path) {
        Cookie cookie = new Cookie(key, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(
            (int) Duration.ofMinutes(15).toSeconds()
        );
        cookie.setPath(path);
        cookie.setAttribute("SameSite", sameSite);      
        return cookie;
    }

}
