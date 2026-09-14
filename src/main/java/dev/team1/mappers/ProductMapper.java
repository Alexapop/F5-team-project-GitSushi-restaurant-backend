package dev.team1.mappers;

import dev.team1.products.ProductEntity;
import dev.team1.products.dtos.ProductDTORequest;
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

    public static ProductEntity toEntity(ProductDTORequest dto) {
        return ProductEntity.builder()
            .name(dto.name())
            .category(dto.category())
            .description(dto.description())
            .price(dto.price())
            .discount(dto.discount())
            .imageUrl(dto.imageUrl())
            .exclusive(dto.exclusive())
            .available(dto.available())
            .build();
    }

}
