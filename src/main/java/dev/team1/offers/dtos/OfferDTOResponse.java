package dev.team1.offers.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import dev.team1.products.dtos.ProductDTOResponse;
import lombok.Builder;

@Builder 
public record OfferDTOResponse(
    Long id,
    boolean used,
    BigDecimal originalPrice,
    BigDecimal discountRate,
    BigDecimal finalPrice,
    UUID coupon,
    String userEmail,
    ProductDTOResponse product
) {

}
