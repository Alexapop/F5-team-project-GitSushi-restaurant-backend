package dev.team1.orders.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import dev.team1.enums.OrderStatus;

public record KitchenOrderDTOResponse(
        Long id,
        OrderStatus status,
        String chefNote,
        LocalDateTime createdAt,
        boolean isDelayed,
        List<KitchenOrderItemDTO> items
) {
    public record KitchenOrderItemDTO(
            String productName,
            BigDecimal quantity
    ) {}
}
