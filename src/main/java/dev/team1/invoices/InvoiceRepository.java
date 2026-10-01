package dev.team1.invoices;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.team1.enums.OrderStatus;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

  Optional<InvoiceEntity> findByInvoiceNumber(UUID invoiceNumber);

  boolean existsByInvoiceNumber(UUID invoiceNumber);

  Page<InvoiceEntity> findByOrder_Status (OrderStatus status, Pageable pageable);
}
