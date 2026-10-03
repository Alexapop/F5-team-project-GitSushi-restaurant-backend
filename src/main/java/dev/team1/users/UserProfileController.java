package dev.team1.users;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.auth.AuthService;
import dev.team1.auth.CustomUserDetails;
import dev.team1.contracts.IUserService;
import dev.team1.security.dtos.JwtAuthenticationDTO;
import dev.team1.users.dtos.UserProfileRequestDTO;
import dev.team1.users.dtos.UserResponseDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "${api-endpoint}/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final IUserService userService;
    private final AuthService authService;

    @Value("/${api-endpoint}/auth/refresh")
    private String refreshPath;

    @Value("${cookie-same-site}")
    private String sameSite;

    @Value("${access-token-duration-minutes}")
    private int accessTokenDurationMinutes;

    @Value("${refresh-token-duration-days}")
    private int refreshTokenDurationDays;

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateProfile(
        @PathVariable UUID id,
        @RequestBody UserProfileRequestDTO requestDTO,
        @AuthenticationPrincipal CustomUserDetails principal,
        HttpServletResponse response
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        }

        UUID authenticatedUserId = principal.user().getId();
        String previousEmail = principal.getUsername();

        UserResponseDTO updatedUser = userService.updateProfile(
            id,
            authenticatedUserId,
            requestDTO
        );

        if (!previousEmail.equals(updatedUser.email())) {
            JwtAuthenticationDTO authentication = authService.getAuth(
                updatedUser.email(),
                updatedUser.roles()
            );

            int accessMaxAge = Math.toIntExact(
                Duration.ofMinutes(accessTokenDurationMinutes).toSeconds()
            );
            int refreshMaxAge = Math.toIntExact(
                Duration.ofDays(refreshTokenDurationDays).toSeconds()
            );

            response.addCookie(createCookie(
                "access_token", authentication.token(), "/", accessMaxAge
            ));
            response.addCookie(createCookie(
                "refresh_token", authentication.refreshToken(), refreshPath, refreshMaxAge
            ));
        }

        return ResponseEntity.ok(updatedUser);
    }

    private Cookie createCookie(String name, String value, String path, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", sameSite);
        return cookie;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidProfile(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDatabaseConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("No se pudo guardar el perfil por un conflicto de datos");
    }
}
