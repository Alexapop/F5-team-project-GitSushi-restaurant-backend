package dev.team1.payments.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentDTORequest(
    @NotNull @Positive Long orderId,
    @Email String email) {

}
