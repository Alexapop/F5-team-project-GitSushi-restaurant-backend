package dev.team1.reports.dtos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import dev.team1.reports.ReportPeriod;

// Datos completos del resumen de ventas para generar el PDF.
public record SalesReport(
    ReportPeriod period,
    LocalDate firstDay,
    LocalDate lastDay,
    BigDecimal revenue,
    long orders,
    List<ChannelSales> channels,
    ZonedDateTime generatedAt
) {

    public BigDecimal averageTicket() {
        if (orders == 0) {
            return BigDecimal.ZERO;
        }
        return revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
    }
}