package dev.team1.products.dtos;

import jakarta.validation.constraints.NotNull;

public record ProductAvailableDTO(
    @NotNull 
    Boolean available
) {

}
