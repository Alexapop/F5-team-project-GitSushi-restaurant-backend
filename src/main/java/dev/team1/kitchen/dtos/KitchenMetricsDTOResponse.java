package dev.team1.kitchen.dtos;

public record KitchenMetricsDTOResponse(
        long totalActiveOrders,
        double averagePreparationMinutes,
        long processingCount,
        long delayedCount,
        long readyCount
) {
}