package dev.team1.kpi.dtos;

import java.math.BigDecimal;

// Ventas de un periodo y del mismo tramo del periodo anterior (para la variación).
public record PeriodSales(
    BigDecimal revenue,
    BigDecimal previousRevenue
) {
}