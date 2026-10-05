package dev.team1.reports.dtos;

import java.math.BigDecimal;

// Totales del periodo que pinta el front: { "revenue": 1234.50, "orders": 42 }.
public record SalesSummaryDTOResponse(
    BigDecimal revenue,
    long orders
) {
}