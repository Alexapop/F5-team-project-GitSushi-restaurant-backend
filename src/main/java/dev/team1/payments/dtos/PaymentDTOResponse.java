package dev.team1.payments.dtos;

import java.math.BigDecimal;

public record PaymentDTOResponse(
    String sessionId,
    String checkoutUrl,
    String paymentStatus,
    Long orderId,
    BigDecimal amount) {

}
