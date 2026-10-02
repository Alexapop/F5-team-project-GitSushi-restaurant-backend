package dev.team1.users.dtos;

public record UserPatchRequestDTO(
    String firstName,
    String lastName,
    String email,
    String address,
    String postalCode,
    String city,
    String role
) {

}
