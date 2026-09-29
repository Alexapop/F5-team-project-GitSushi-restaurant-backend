package dev.team1.security.dtos;

import lombok.Builder;

@Builder 
public record JwtAuthenticationDTO(
    String token, 
    String refreshToken
) {
    
}
