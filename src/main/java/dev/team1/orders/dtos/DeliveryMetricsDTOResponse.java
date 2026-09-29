package dev.team1.orders.dtos;

public record DeliveryMetricsDTOResponse(
        long readyCount,
        long inTransitCount,
        long deliveredTodayCount,
        double averageDeliveryMinutes
) {
}
