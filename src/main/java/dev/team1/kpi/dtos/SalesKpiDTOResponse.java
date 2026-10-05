package dev.team1.kpi.dtos;

import java.time.DayOfWeek;
import java.util.List;

// KPI de ventas que pinta el panel de administración (GET /api/v1/kpi/sales).
public record SalesKpiDTOResponse(
    PeriodSales today,
    PeriodSales month,
    PeriodSales quarter,
    PeriodSales year,
    ChannelSplit channels,
    List<DailySales> weekly,
    DayOfWeek peakDay
) {
}