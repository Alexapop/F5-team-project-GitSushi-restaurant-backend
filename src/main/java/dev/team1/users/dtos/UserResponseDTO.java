package dev.team1.users.dtos;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder 
public record UserResponseDTO(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String address,
    String postalCode,
    String city,
    List<String> roles,
    boolean active
) {
    @JsonProperty("profileCompletion")
    public int profileCompletion() {
        String[] requiredFields = {
            firstName,
            lastName,
            email,
            address,
            postalCode,
            city
        };

        int completedFields = 0;

        for (String field : requiredFields) {
            if (field != null && !field.trim().isEmpty()) {
                completedFields++;
            }
        }

        return completedFields * 100 / requiredFields.length;
    }
}
