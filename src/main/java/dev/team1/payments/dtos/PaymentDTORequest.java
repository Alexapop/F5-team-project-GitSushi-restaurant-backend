package dev.team1.payments.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// El cliente solo envía el pedido a pagar; el importe se calcula en el backend.
// El email es opcional porque un invitado puede pagar sin tener cuenta.
public record PaymentDTORequest(
    @NotNull @Positive Long orderId,
    @Email String email) {

}
