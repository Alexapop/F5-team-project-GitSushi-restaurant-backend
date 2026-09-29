package dev.team1.offers;

import java.math.BigDecimal;
import java.util.UUID;

import dev.team1.products.ProductEntity;
import dev.team1.users.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity 
@Table(name = "offers")
@NoArgsConstructor 
@Data
public class OfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "discount_rate")
    private BigDecimal discountRate;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Column(name = "coupon", unique = true, nullable = false)
    private UUID coupon;

    private boolean used = false;


    @ManyToOne 
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    @OneToOne
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @Builder
    public OfferEntity(BigDecimal originalPrice, BigDecimal discountRate, BigDecimal finalPrice, UserEntity user,
            ProductEntity product) {
        this.originalPrice = originalPrice;
        this.discountRate = discountRate;
        this.finalPrice = finalPrice;
        this.user = user;
        this.product = product;
    }

    @PrePersist
    protected void onCreate() {
        this.coupon = UUID.randomUUID();

        if (finalPrice == null && originalPrice != null && discountRate != null) {
            this.finalPrice = originalPrice.subtract(
                originalPrice.multiply(
                    discountRate.divide(
                        BigDecimal.valueOf(100)
                    )
                )
            );
        }
    }

}
