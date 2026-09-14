package dev.team1.products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import dev.team1.enums.ProductCategory;
import dev.team1.products.dtos.ProductDTOResponse;

public class ProductTestData {

    static List<ProductDTOResponse> sampleDTOs() {
        return List.of(
            ProductDTOResponse.builder()
                .available(true)
                .id(1L)
                .name("Ramen Fix")
                .description("This ramen will fix all the bugs.")
                .category(ProductCategory.ENTRANTES)
                .imageUrl("ramen-fix.png")
                .discount(BigDecimal.valueOf(0.0))
                .price(BigDecimal.valueOf(5.0))
                .exclusive(false)
                .build(),
            ProductDTOResponse.builder()
                .available(false)
                .id(2L)
                .name("Baked Python")
                .description("This baked snake will make you nervious because of bug with datatypes.")
                .category(ProductCategory.ENTRANTES)
                .imageUrl("baked-python.png")
                .discount(BigDecimal.valueOf(0.0))
                .price(BigDecimal.valueOf(5.0))
                .exclusive(false)
                .build(),
            ProductDTOResponse.builder()
                .available(true)
                .id(3L)
                .name("Gowok Golang")
                .description("This wok will go to your mouth very fast.")
                .category(ProductCategory.ARROZ_AND_FIDEOS)
                .imageUrl("gowok-golang.png")
                .discount(BigDecimal.valueOf(0.0))
                .price(BigDecimal.valueOf(5.0))
                .exclusive(false)
                .build(),
            ProductDTOResponse.builder()
                .available(true)
                .id(4L)
                .name("Java Roll")
                .description("This roll will make you secure and confident in result.")
                .category(ProductCategory.ROLLS)
                .imageUrl("java-roll.png")
                .discount(BigDecimal.valueOf(0.0))
                .price(BigDecimal.valueOf(5.0))
                .exclusive(false)
                .build(),
            ProductDTOResponse.builder()
                .available(true)
                .id(5L)
                .name("Sysalmon")
                .description("This salmon nigiri will configure your systems.")
                .category(ProductCategory.NIGIRI)
                .imageUrl("sysalmon.png")
                .discount(BigDecimal.valueOf(0.0))
                .price(BigDecimal.valueOf(5.0))
                .exclusive(false)
                .build(),
            ProductDTOResponse.builder()
                .available(true)
                .id(6L)
                .name("EbiOps")
                .description("This ebi nigiri will start all your servers.")
                .category(ProductCategory.NIGIRI)
                .imageUrl("ebi-ops.png")
                .discount(BigDecimal.valueOf(0.0))
                .price(BigDecimal.valueOf(5.0))
                .exclusive(false)
                .build(),
            ProductDTOResponse.builder()
                .available(true)
                .id(7L)
                .name("Mochi Python")
                .description("This mochi will be in some movie.")
                .category(ProductCategory.POSTRES)
                .imageUrl("mochi-python.png")
                .discount(BigDecimal.valueOf(0.0))
                .price(BigDecimal.valueOf(5.0))
                .exclusive(false)
                .build(),
            ProductDTOResponse.builder()
                .available(true)
                .id(8L)
                .name("Browney++")
                .description("This browney will be the foundation of other dishes.")
                .category(ProductCategory.POSTRES)
                .imageUrl("browney.png")
                .discount(BigDecimal.valueOf(0.0))
                .price(BigDecimal.valueOf(5.0))
                .exclusive(false)
                .build()
        );
    }

    static List<ProductEntity> sampleEntities() {
        return List.of(
            new ProductEntity(
                1L, "Ramen Fix", 
                ProductCategory.ENTRANTES, 
                "This ramen will fix all the bugs", 
                "ramen-fix.png", BigDecimal.valueOf(5.0), 
                BigDecimal.valueOf(0.0), 
                true, false, new ArrayList<>()
            ),
            new ProductEntity(
                2L, "Baked Python", 
                ProductCategory.ENTRANTES, 
                "This baked snake will make you nervious because of bug with datatypes.", 
                "baked-python.png", BigDecimal.valueOf(5.0), 
                BigDecimal.valueOf(0.0), 
                false, false, new ArrayList<>()
            ),
            new ProductEntity(
                3L, "Gowok Golang", 
                ProductCategory.ARROZ_AND_FIDEOS, 
                "This wok will go to your mouth very fast.", 
                "gowok-golang.png", BigDecimal.valueOf(5.0), 
                BigDecimal.valueOf(0.0), 
                true, false, new ArrayList<>()
            ),
            new ProductEntity(
                4L, "Java Roll", 
                ProductCategory.ROLLS, 
                "This roll will make you secure and confident in result.", 
                "java-roll.png", BigDecimal.valueOf(5.0), 
                BigDecimal.valueOf(0.0), 
                true, false, new ArrayList<>()
            ),
            new ProductEntity(
                5L, "Sysalmon", 
                ProductCategory.NIGIRI, 
                "This salmon nigiri will configure your systems.", 
                "sysalmon.png", BigDecimal.valueOf(5.0), 
                BigDecimal.valueOf(0.0), 
                true, false, new ArrayList<>()
            ),
            new ProductEntity(
                6L, "EbiOps", 
                ProductCategory.NIGIRI, 
                "This ebi nigiri will start all your servers.", 
                "ebi-ops.png", BigDecimal.valueOf(5.0), 
                BigDecimal.valueOf(0.0), 
                true, false, new ArrayList<>()
            ),
            new ProductEntity(
                7L, "Mochi Python", 
                ProductCategory.POSTRES, 
                "This mochi will be in some movie.", 
                "mochi-python.png", BigDecimal.valueOf(5.0), 
                BigDecimal.valueOf(0.0), 
                true, false, new ArrayList<>()
            ),
            new ProductEntity(
                8L, "Browney++", 
                ProductCategory.POSTRES, 
                "This browney will be the foundation of other dishes.", 
                "browney.png", BigDecimal.valueOf(5.0), 
                BigDecimal.valueOf(0.0), 
                true, false, new ArrayList<>()
            )
        );
    }

}
