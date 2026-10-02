package dev.team1.mappers;

import dev.team1.invoices.InvoiceEntity;
import dev.team1.invoices.dtos.InvoiceDTORequest;
import dev.team1.invoices.dtos.InvoiceDTOResponse;
import dev.team1.invoices.dtos.PaidInvoiceDTOResponse;
import dev.team1.orders.OrderEntity;

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

  public static PaidInvoiceDTOResponse toPaidDTO(InvoiceEntity invoice) {
    OrderEntity order = invoice.getOrder();

    String customerName = null;
    Integer tableNumber = null;

    if(order != null) {
      if(order.getUser() != null) {
        customerName = order.getUser().getFirstName() + " " + order.getUser().getLastName();
      }

      if(order.getTable() != null) {
        tableNumber = order.getTable().getTableNumber();
      }
    }

    return new PaidInvoiceDTOResponse(
      invoice.getId(),
      invoice.getInvoiceNumber(),
      customerName,
      tableNumber,
      order != null ? order.getChannel() : null,
      invoice.getAmount(),
      order != null ? order.getStatus() : null,
      order != null ? order.getPaymentMethod() : null,
      invoice.getPaidAt()
    );
  }
}
