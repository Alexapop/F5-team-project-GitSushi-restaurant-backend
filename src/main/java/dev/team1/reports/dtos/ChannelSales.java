package dev.team1.reports.dtos;

import java.math.BigDecimal;

import dev.team1.enums.OrderChannel;

// Ventas de un canal (sala o domicilio) dentro del periodo.
public record ChannelSales(
    OrderChannel channel,
    long orders,
    BigDecimal revenue
) {
}