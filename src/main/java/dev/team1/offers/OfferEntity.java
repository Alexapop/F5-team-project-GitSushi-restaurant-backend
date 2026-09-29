package dev.team1.offers;

import java.math.BigDecimal;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private boolean used = false;

    @ManyToOne 
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToOne
    @JoinColumn(name = "product_id")
    private ProductEntity product;
    
}
