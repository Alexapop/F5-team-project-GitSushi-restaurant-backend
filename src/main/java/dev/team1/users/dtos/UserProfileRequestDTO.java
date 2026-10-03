package dev.team1.users.dtos;

public record UserProfileRequestDTO(
    String firstName,
    String lastName,
    String email,
    String address,
    String postalCode,
    String city
) {
    
}
