package dev.team1.orders.dtos;

import java.math.BigDecimal;

public record OrderDTOResponse(
        Long id,
        BigDecimal subtotal,
        Integer discountRate,
        BigDecimal discountAmount,
        Integer vatRate,
        BigDecimal total,
        BigDecimal vatAmount,
        String chefNote

) {

}
