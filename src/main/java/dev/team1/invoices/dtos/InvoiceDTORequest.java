package dev.team1.invoices.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record InvoiceDTORequest(
  @NotNull(message = "El campo 'invoice_number no debe de ser vacío")
  UUID invoiceNumber,

  @NotNull(message = "El campo 'amount' es obligatorio")
  @DecimalMin (value = "0.0", inclusive = false)
  @Digits(integer = 10, fraction = 2, message = "El campo 'amount' debe estar en formato 0.00 con 10 dígitos máximo en un número y dos dígitos después de punto decimal")
  BigDecimal amount,

  @NotNull(message = "El campo 'paidAt' es obligatorio")
  @PastOrPresent(message = "La fecha de pago no puede ser futura")
  Instant paidAt
) {

}
