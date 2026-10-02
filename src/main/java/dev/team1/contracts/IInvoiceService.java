package dev.team1.contracts;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.team1.invoices.dtos.InvoiceDTORequest;
import dev.team1.invoices.dtos.InvoiceDTOResponse;
import dev.team1.invoices.dtos.PaidInvoiceDTOResponse;

public interface IInvoiceService {
  InvoiceDTOResponse create(InvoiceDTORequest request);
  InvoiceDTOResponse findById(Long id);
  PaidInvoiceDTOResponse findPaidById(Long id);
  Page<InvoiceDTOResponse> findAll(Pageable pageable);
  Page<PaidInvoiceDTOResponse> findPaid(Pageable pageable);
  @Nullable
  Object findPaid(String search, Pageable pageable);
}
