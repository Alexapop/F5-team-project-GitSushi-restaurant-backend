package dev.team1.products.dtos;

import java.math.BigDecimal;

import dev.team1.enums.ProductCategory;
import lombok.Builder;

@Builder 
public record ProductDTOResponse(
    Long id,
    String name,
    ProductCategory category,
    String description,
    String imageUrl,
    BigDecimal price,
    BigDecimal discount,
    boolean available,
    boolean exclusive
) {

}
