package dev.team1.invoices.dtos;

import java.math.BigDecimal;
import java.time.Instant;

import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;
import dev.team1.enums.PaymentMethod;

public record PaidInvoiceDTOResponse(
  Long invoiceId,
  String customerName,
  Integer tableNumber,
  OrderChannel channel,
  BigDecimal amount,
  OrderStatus status,
  PaymentMethod paymentMethod,
  Instant paidAt
) {
  
}
