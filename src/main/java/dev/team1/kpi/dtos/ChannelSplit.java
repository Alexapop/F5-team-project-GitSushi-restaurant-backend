package dev.team1.kpi.dtos;

// Reparto de las ventas del mes entre sala y domicilio.
public record ChannelSplit(
    ChannelShare inStore,
    ChannelShare delivery
) {
}