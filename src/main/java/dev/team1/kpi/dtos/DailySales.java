package dev.team1.kpi.dtos;

import java.math.BigDecimal;
import java.time.DayOfWeek;

// Ventas de un día de la semana actual, separadas por canal.
public record DailySales(
    DayOfWeek day,
    BigDecimal inStore,
    BigDecimal delivery
) {
}