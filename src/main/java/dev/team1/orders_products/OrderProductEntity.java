package dev.team1.orders_products;


import java.math.BigDecimal;

import dev.team1.orders.OrderEntity;
import dev.team1.products.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "orders_products")
@NoArgsConstructor 
@Getter 
public class OrderProductEntity {

    @EmbeddedId 
    private OrderProductId id = new OrderProductId(); // we create an object here becauese MapsId doesn't create it itself.

    @ManyToOne 
    @MapsId("orderId")
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @ManyToOne 
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Builder 
    public OrderProductEntity(OrderEntity order, ProductEntity product, BigDecimal quantity) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
    }

}
