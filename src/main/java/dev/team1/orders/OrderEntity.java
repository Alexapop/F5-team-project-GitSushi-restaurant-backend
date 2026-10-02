package dev.team1.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.FetchType;
import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;
import dev.team1.enums.PaymentStatus;
import dev.team1.orders_products.OrderProductEntity;
import dev.team1.tables.TableEntity;
import dev.team1.users.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    @Column(name = "id_order")
    private Long id;

    @Column(nullable = false, scale = 2)
    private BigDecimal subtotal;

    private Integer discountRate;// for discount percentage (e.g.5%)

    @Column(nullable = false, scale = 2) // for the discount amount (e.g. 2.5 euro)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private Integer vatRate;

    @Column(nullable = false, scale = 2)
    private BigDecimal vatAmount;

    @Column(nullable = false, scale = 2)
    private BigDecimal total;
    
 //added delivery details 
    @Column(precision = 19, scale = 2, updatable = false)
    private BigDecimal deliveryFee;

    @Column(length = 255, updatable = false)
    private String deliveryStreet;

    @Column(length = 100, updatable = false)
    private String deliveryCity;

    @Column(length = 20, updatable = false)
    private String deliveryPostalCode;

    @Column(length = 500, updatable = false)
    private String deliveryInstructions;

    @Column(length = 36, unique = true, updatable = false)
    private String ticketAccessToken;
    
    @Column (length = 500, nullable = true)
    private String chefNote;

    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProductEntity> orderProducts = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "id_table")
    private TableEntity table;

    // GS-341: usuario que hizo el pedido; es null cuando el pedido lo hace un invitado.
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private UserEntity user;

        // GS-668: motorista asignado al pedido; null hasta que un repartidor se lo asigne.
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "deliveryman_id", nullable = true)
    private UserEntity deliveryman;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

        // GS-707: bloqueo optimista para evitar dobles asignaciones del mismo pedido.
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
