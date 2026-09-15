package dev.team1.orders.dtos;

import dev.team1.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record KitchenStatusUpdateDTORequest(
        @NotNull OrderStatus status
) {
}
