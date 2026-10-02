package dev.team1.delivery.dtos;

public record DeliveryMetricsDTOResponse(
        long readyCount,
        long inTransitCount,
        long deliveredTodayCount,
        double averageDeliveryMinutes
) {
}
