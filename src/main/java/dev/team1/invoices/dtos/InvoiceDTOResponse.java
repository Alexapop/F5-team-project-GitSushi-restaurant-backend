package dev.team1.invoices.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder 
public record InvoiceDTOResponse(
  Long id,
  Long orderId,
  UUID invoiceNumber,
  BigDecimal amount,
  Instant paidAt
) {

}
