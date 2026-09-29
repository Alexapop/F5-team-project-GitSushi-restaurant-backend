package dev.team1.mappers;

import dev.team1.invoices.InvoiceEntity;
import dev.team1.invoices.dtos.InvoiceDTORequest;
import dev.team1.invoices.dtos.InvoiceDTOResponse;

public class InvoiceMapper {

  private InvoiceMapper() {
  }

  public static InvoiceDTOResponse toDTO(InvoiceEntity entity) {
    return InvoiceDTOResponse.builder()
      .id(entity.getId())
      .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
      .invoiceNumber(entity.getInvoiceNumber())
      .amount(entity.getAmount())
      .paidAt(entity.getPaidAt())
      .build()
    ;
  }

  public static InvoiceEntity toEntity(InvoiceDTORequest dto) {
    return InvoiceEntity.builder()
      .amount(dto.amount())
      .paidAt(dto.paidAt())
      .build()
    ;
  }
}
