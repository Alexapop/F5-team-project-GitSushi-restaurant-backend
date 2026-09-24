package dev.team1.users.dtos;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder 
public record UserResponseDTO(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String address,
    String postalCode,
    String city,
    List<String> roles
) {
}
