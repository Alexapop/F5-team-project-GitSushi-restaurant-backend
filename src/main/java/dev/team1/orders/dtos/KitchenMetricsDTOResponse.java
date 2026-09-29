package dev.team1.orders.dtos;

public record KitchenMetricsDTOResponse(
        long totalActiveOrders,
        double averagePreparationMinutes,
        long processingCount,
        long delayedCount,
        long readyCount
) {
}