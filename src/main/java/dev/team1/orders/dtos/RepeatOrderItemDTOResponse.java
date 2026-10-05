package dev.team1.orders.dtos;

import java.math.BigDecimal;

// GS-581: línea de un pedido anterior lista para cargar en la cesta (precio actual del producto).
public record RepeatOrderItemDTOResponse(
        Long productId,
        String name,
        BigDecimal price,
        int quantity
) {
}
