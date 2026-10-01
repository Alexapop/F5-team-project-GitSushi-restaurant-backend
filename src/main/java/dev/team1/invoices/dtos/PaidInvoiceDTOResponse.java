package dev.team1.invoices.dtos;

import java.math.BigDecimal;
import java.time.Instant;

import dev.team1.enums.OrderChannel;
import dev.team1.enums.OrderStatus;

public record PaidInvoiceDTOResponse(
  Long invoiceId,
  String customerName,
  int tableNumber,
  OrderChannel channel,
  BigDecimal amount,
  OrderStatus status,
  Instant paidAt
) {
  
}
