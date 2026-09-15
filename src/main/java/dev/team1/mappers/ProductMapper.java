package dev.team1.mappers;

import dev.team1.products.ProductEntity;
import dev.team1.products.dtos.ProductDTOPatchRequest;
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

    public static ProductEntity updateEntity(ProductEntity entity, ProductDTOPatchRequest dto) {
        
        if (dto.name() != null) entity.setName(dto.name());
        if (dto.category() != null) entity.setCategory(dto.category());
        if (dto.description() != null) entity.setDescription(dto.description());
        if (dto.price() != null) entity.setPrice(dto.price());
        if (dto.discount() != null) entity.setDiscount(dto.discount());
        if (dto.imageUrl() != null) entity.setImageUrl(dto.imageUrl());
        if (dto.available() != null) entity.setAvailable(dto.available());
        if (dto.exclusive() != null) entity.setExclusive(dto.exclusive());

        return entity;
    }

}
