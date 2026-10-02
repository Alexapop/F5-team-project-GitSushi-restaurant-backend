package dev.team1.tickets.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import dev.team1.delivery.dtos.DeliveryAddressDTOResponse;
import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.enums.PaymentStatus;

public record TicketDTOResponse(
        Long id,
        OrderStatus status,
        OrderChannel channel,
        Integer tableNumber,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        List<TicketItemDTO> items,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        Integer vatRate,
        BigDecimal vatAmount,
        BigDecimal deliveryFee,
        BigDecimal total,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        DeliveryAddressDTOResponse deliveryAddress // null para pedidos en sala
) {
     public record TicketItemDTO(
            String productName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}
}
