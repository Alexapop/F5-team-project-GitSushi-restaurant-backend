package dev.team1.kitchen.dtos;

public record KitchenChannelCountsDTOResponse(
        long total,
        long inStore,
        long delivery
) {
}
