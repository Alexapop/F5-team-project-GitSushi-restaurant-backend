package dev.team1.orders_products;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.NoArgsConstructor;

@Embeddable 
@NoArgsConstructor 
public class OrderProductId implements Serializable {

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "product_id")
    private Long productId;

    @Override
    public int hashCode() {
        return Objects.hash(orderId, productId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OrderProductId)) return false;
        OrderProductId other = (OrderProductId) obj;
        return Objects.equals(orderId, other.orderId) && Objects.equals(productId, other.productId);
    }

}
