package dev.team1.kpi.dtos;

import java.math.BigDecimal;

// Ventas de un canal y su porcentaje sobre el total (entero de 0 a 100).
public record ChannelShare(
    BigDecimal revenue,
    int percentage
) {
}