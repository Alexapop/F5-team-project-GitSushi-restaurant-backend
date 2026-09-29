package dev.team1.contracts;

import dev.team1.invoices.dtos.InvoiceDTORequest;
import dev.team1.invoices.dtos.InvoiceDTOResponse;

public interface IInvoiceService {
  InvoiceDTOResponse create(InvoiceDTORequest request);
  InvoiceDTOResponse findById(Long id);
}
