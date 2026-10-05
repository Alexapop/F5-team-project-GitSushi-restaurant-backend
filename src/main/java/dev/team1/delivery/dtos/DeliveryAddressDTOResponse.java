package dev.team1.delivery.dtos;

public record DeliveryAddressDTOResponse(
    String deliveryStreet,
    String deliveryCity,
    String deliveryPostalCode,
    String deliveryInstructions
) {
    
}
