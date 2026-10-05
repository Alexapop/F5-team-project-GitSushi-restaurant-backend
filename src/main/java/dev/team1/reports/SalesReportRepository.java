package dev.team1.reports;

import java.time.Instant;
import java.util.List;

import org.springframework.data.repository.Repository;

import dev.team1.invoices.InvoiceEntity;

// Consulta de facturas para el resumen de ventas. Va aparte de InvoiceRepository
// para no mezclar el resumen con la gestión de facturas.
public interface SalesReportRepository extends Repository<InvoiceEntity, Long> {

    // Facturas pagadas en [from, to): incluye "from" y excluye "to".
    List<InvoiceEntity> findByPaidAtGreaterThanEqualAndPaidAtLessThan(Instant from, Instant to);
}