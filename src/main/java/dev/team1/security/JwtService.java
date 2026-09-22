package dev.team1.security;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.team1.security.dtos.JwtAuthentificationDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component 
public class JwtService {

    @Value("${jwt-secret}") // 0kuSAcoQ7qpnLOO92S2VCrkc8g2tiDrWgZ5V+lzYWqA=
    private String jwtSecret;

    public JwtAuthentificationDTO generateAuthToken(String email, String role) {
        return JwtAuthentificationDTO.builder()
            .token(generateJwtToken(email, role))
            .refreshToken(generateRefreshToken(email, role))
            .build();
    }

    public JwtAuthentificationDTO refreshBaseToken(String email, String role, String refreshToken) {
        return JwtAuthentificationDTO.builder()
            .token(generateJwtToken(email, role))
            .refreshToken(refreshToken)
            .build();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.getSubject();
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();

        String rolesString = claims.get("role", String.class);
        return List.of(rolesString.split(", "));
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return true;
            
        } catch (Exception exc) {
            return false; 
        }
    }

    private String generateJwtToken(String email, String role) {
        Date date = Date.from(
            LocalDateTime.now()
            .plusMinutes(1)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        );
        Date issuedAt = Date.from(
            LocalDateTime.now()
            .atZone(ZoneId.systemDefault())
            .toInstant()
        );

        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .issuedAt(issuedAt)
            .expiration(date)
            .signWith(getSignKey())
            .compact();
    }

    private String generateRefreshToken(String email, String role) {
        Date date = Date.from(
            LocalDateTime.now()
            .plusDays(1)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        );
        Date issuedAt = Date.from(
            LocalDateTime.now()
            .atZone(ZoneId.systemDefault())
            .toInstant()
        );

        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .issuedAt(issuedAt)
            .expiration(date)
            .signWith(getSignKey())
            .compact();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}

