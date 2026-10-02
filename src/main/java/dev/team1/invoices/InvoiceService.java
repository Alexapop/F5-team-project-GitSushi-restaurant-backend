package dev.team1.invoices;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.team1.contracts.IInvoiceService;
import dev.team1.enums.OrderStatus;
import dev.team1.invoices.dtos.InvoiceDTORequest;
import dev.team1.invoices.dtos.InvoiceDTOResponse;
import dev.team1.invoices.dtos.PaidInvoiceDTOResponse;
import dev.team1.invoices.exceptions.InvoiceException;
import dev.team1.invoices.exceptions.InvoiceExceptionNotFound;
import dev.team1.mappers.InvoiceMapper;

@Service
public class InvoiceService implements IInvoiceService {
  private final InvoiceRepository invoiceRepository;

  public InvoiceService(InvoiceRepository invoiceRepository) {
    this.invoiceRepository = invoiceRepository;
  }

  @Override
  @Transactional
  public InvoiceDTOResponse create(InvoiceDTORequest request) {
    if (invoiceRepository.existsByInvoiceNumber(request.invoiceNumber())) {
      throw new InvoiceException(
          "Invoice number " + request.invoiceNumber() + " already exists.");
    }

    InvoiceEntity invoice = InvoiceMapper.toEntity(request);
    invoice.setInvoiceNumber(request.invoiceNumber());

    InvoiceEntity savedInvoice = invoiceRepository.save(invoice);
    return InvoiceMapper.toDTO(savedInvoice);
  }

  @Override
  @Transactional(readOnly = true)
  public InvoiceDTOResponse findById(Long id) {
    InvoiceEntity invoice = invoiceRepository.findById(id)
        .orElseThrow(() -> new InvoiceExceptionNotFound(
            "Invoice " + id + " not found."));

    return InvoiceMapper.toDTO(invoice);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<InvoiceDTOResponse> findAll(Pageable pageable) {
    return invoiceRepository.findAll(pageable)
      .map(InvoiceMapper::toDTO);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PaidInvoiceDTOResponse> findPaid(Pageable pageable) {
    return invoiceRepository
      .findByOrder_Status(OrderStatus.PAID, pageable)
      .map(InvoiceMapper::toPaidDTO);
  }
}
