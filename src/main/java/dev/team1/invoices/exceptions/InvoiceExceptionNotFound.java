package dev.team1.invoices.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Invoice not found")
public class InvoiceExceptionNotFound extends InvoiceException {
  public InvoiceExceptionNotFound(String message) {
    super(message);
  }
}
