package dev.team1.invoices;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import dev.team1.orders.OrderEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table(name = "invoices")
@Getter 
@Setter 
public class InvoiceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_invoice", nullable = false)
  @Setter(AccessLevel.NONE)
  private Long id;

  @OneToOne 
  @JoinColumn(name = "id_order")
  private OrderEntity order;
  
  @UuidGenerator
  @Column(name = "invoice_number", nullable = false, unique = true)
  private UUID invoiceNumber;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "paid_at", nullable = false)
  private Instant paidAt;

  @Builder
  public InvoiceEntity(BigDecimal amount, Instant paidAt) {
    this.amount = amount;
    this.paidAt = paidAt;
  }
}
