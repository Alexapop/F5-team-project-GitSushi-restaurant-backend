package dev.team1.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record CredentialsDTO(
    @NotBlank 
    String email,

    @NotBlank 
    String password
) {

}
