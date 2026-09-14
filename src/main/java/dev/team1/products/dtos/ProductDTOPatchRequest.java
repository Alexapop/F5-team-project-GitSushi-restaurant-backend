package dev.team1.products.dtos;

import java.math.BigDecimal;

import dev.team1.enums.ProductCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

public record ProductDTOPatchRequest(
    String name,

    ProductCategory category,
    
    String description,
    
    String imageUrl,
    
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2, message = "El campo 'price' debe estar en formato 0.00 con 10 dígitos máximo en un número y dos dígitos después de punto decimal")
    BigDecimal price,
    
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "100.0")
    @Digits(integer = 3, fraction = 2, message = "El campo 'discount' debe estar en formato de porcentaje")
    BigDecimal discount,
    
    Boolean available,
    
    Boolean exclusive

) {

}

