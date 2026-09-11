package dev.team1.products.dtos;

import java.math.BigDecimal;

import dev.team1.enums.ProductCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

public record ProductDTORequest(
    @NotBlank(message = "EL campo 'name' no debe estar vacío")
    String name,

    @NonNull
    ProductCategory category,
    
    @NotBlank(message = "EL campo 'description' no debe estar vacío")
    String description,
    
    @NotBlank(message = "EL campo 'imageUrl' no debe estar vacío")
    String imageUrl,
    
    @NonNull 
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2, message = "El campo 'price' debe estar en formato 0.00 con 10 dígitos máximo en un número y dos dígitos después de punto decimal")
    BigDecimal price,
    
    @NonNull 
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "100.0")
    @Digits(integer = 3, fraction = 2, message = "El campo 'discount' debe estar en formato de porcentaje")
    BigDecimal discount,
    
    boolean available,
    
    boolean exclusive
) {

}
