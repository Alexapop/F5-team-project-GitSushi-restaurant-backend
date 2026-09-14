package dev.team1.orders.dtos;

import java.math.BigDecimal;

import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;

public record OrderDTOResponse(
        Long id,
        BigDecimal subtotal,
        Integer discountRate,
        BigDecimal discountAmount,
        Integer vatRate,
        BigDecimal total,
        BigDecimal vatAmount,
        String chefNote,
        OrderStatus status,
        OrderChannel channel,
        PaymentMethod paymentMethod

) {

}
