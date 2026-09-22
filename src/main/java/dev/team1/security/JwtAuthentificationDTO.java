package dev.team1.security;

import lombok.Builder;

@Builder 
public record JwtAuthentificationDTO(
    String token, 
    String refreshToken
) {
    
}
