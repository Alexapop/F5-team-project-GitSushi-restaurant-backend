package dev.team1.payments.dtos;

import java.math.BigDecimal;

// Datos de la sesión de Stripe; el frontend redirige al cliente a checkoutUrl.
public record PaymentDTOResponse(
    String sessionId,
    String checkoutUrl,
    String paymentStatus,
    Long orderId,
    BigDecimal amount) {

}
