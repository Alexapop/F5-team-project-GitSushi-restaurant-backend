package dev.team1.mappers;

import dev.team1.offers.OfferEntity;
import dev.team1.offers.dtos.OfferDTOResponse;

public class OfferMapper {

    private OfferMapper() {}

    public static OfferDTOResponse toDTO(OfferEntity entity) {
        return OfferDTOResponse.builder()
            .id(entity.getId())
            .used(entity.isUsed())
            .coupon(entity.getCoupon())
            .originalPrice(entity.getOriginalPrice())
            .finalPrice(entity.getFinalPrice())
            .discountRate(entity.getDiscountRate())
            .userEmail(entity.getUser().getEmail())
            .product(ProductMapper.toDTO(entity.getProduct()))
        .build();
    }
}
