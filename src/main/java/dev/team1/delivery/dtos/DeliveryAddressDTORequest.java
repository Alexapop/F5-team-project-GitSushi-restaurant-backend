package dev.team1.delivery.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryAddressDTORequest(
    @NotBlank 
    @Size (max = 255)
    String deliveryStreet,

    @NotBlank 
    @Size (max = 100)
    String deliveryCity,

    @NotBlank 
    @Size(max = 20)
    String deliveryPostalCode,

    @Size(max = 500)
    String deliveryInstructions

    ) {
}
