package dev.team1.invoices;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

  Optional<InvoiceEntity> findByInvoiceNumber(UUID invoiceNumber);

  boolean existsByInvoiceNumber(UUID invoiceNumber);
}
