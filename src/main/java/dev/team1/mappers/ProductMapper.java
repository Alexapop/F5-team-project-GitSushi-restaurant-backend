package dev.team1.mappers;

import dev.team1.products.ProductEntity;
import dev.team1.products.dtos.ProductDTOResponse;

public class ProductMapper {

    private ProductMapper() {}

    public static ProductDTOResponse toDTO(ProductEntity entity) {

        return ProductDTOResponse.builder()
            .id(entity.getId())    
            .name(entity.getName())
            .category(entity.getCategory())
            .imageUrl(entity.getImageUrl())
            .description(entity.getDescription())
            .price(entity.getPrice())
            .discount(entity.getDiscount())
            .available(entity.isAvailable())
            .exclusive(entity.isExclusive())
            .build();
    }

}
