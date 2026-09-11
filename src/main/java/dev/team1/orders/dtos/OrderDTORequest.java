package dev.team1.orders.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import java.util.List;

// order creation request with cart items and an optional chef note.
public record OrderDTORequest(
        @NotEmpty List<@NotNull @Valid OrderItemDTORequest> items,
        String chefNote
) {

    // one ordered product and its quantity.
    public record OrderItemDTORequest(
            @NotNull @Positive Long productId,
            @NotNull @Positive Integer quantity
    ) {
    }
}
