package dev.team1.orders.dtos;

public record UserProfileRequestDTO(
    String firstName,
    String lastName,
    String email,
    String address,
    String postalCode,
    String city
) {
    
}
