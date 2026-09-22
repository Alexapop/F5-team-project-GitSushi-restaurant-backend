package dev.team1.security.dtos;

import lombok.Builder;

@Builder 
public record JwtAuthentificationDTO(
    String token, 
    String refreshToken
) {
    
}
