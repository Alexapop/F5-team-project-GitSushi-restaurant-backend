package dev.team1.products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import dev.team1.enums.ProductCategory;
import dev.team1.orders_products.OrderProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@NoArgsConstructor 
@AllArgsConstructor
@Getter
@Setter 
public class ProductEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "discount", nullable = true)
    private BigDecimal discount; // para porcentaje !!!

    @Column(name = "available")
    private boolean available = true;

    @Column(name = "exclusive")
    private boolean exclusive = false;

    @OneToMany(mappedBy = "product")
    private List<OrderProductEntity> orderProducts = new ArrayList<>();

    @Builder 
    public ProductEntity(String name, ProductCategory category, String description, String imageUrl, BigDecimal price,
            BigDecimal discount, boolean available, boolean exclusive) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.discount = discount;
        this.available = available;
        this.exclusive = exclusive;
    }

}
