package dev.team1.orders.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderHistoryDTOResponse(
        Long id,
        LocalDateTime date,
        List<OrderHistoryItemDTO> items,
        BigDecimal total
) {
    public record OrderHistoryItemDTO(
            Long productId,
            String name,
            int quantity,
            BigDecimal price,
            boolean available
    ) {}
}
