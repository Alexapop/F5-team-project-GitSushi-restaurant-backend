package dev.team1.reports;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.team1.contracts.ISalesReportService;
import dev.team1.enums.OrderChannel;
import dev.team1.invoices.InvoiceEntity;
import dev.team1.reports.dtos.ChannelSales;
import dev.team1.reports.dtos.SalesReport;
import dev.team1.reports.dtos.SalesSummaryDTOResponse;

// Calcula el resumen de ventas de un periodo a partir de las facturas pagadas.
@Service
public class SalesReportService implements ISalesReportService {

    private final SalesReportRepository salesReportRepository;
    private final Clock clock;

    public SalesReportService(SalesReportRepository salesReportRepository, Clock clock) {
        this.salesReportRepository = salesReportRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public SalesSummaryDTOResponse getSummary(ReportPeriod period) {
        List<InvoiceEntity> invoices = findInvoices(period, today());
        return new SalesSummaryDTOResponse(sumAmounts(invoices), invoices.size());
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReport getReport(ReportPeriod period) {
        LocalDate today = today();
        List<InvoiceEntity> invoices = findInvoices(period, today);

        List<ChannelSales> channels = Arrays.stream(OrderChannel.values())
            .map(channel -> {
                List<InvoiceEntity> channelInvoices = invoices.stream()
                    .filter(invoice -> invoice.getOrder() != null
                        && invoice.getOrder().getChannel() == channel)
                    .toList();
                return new ChannelSales(channel, channelInvoices.size(), sumAmounts(channelInvoices));
            })
            .toList();

        return new SalesReport(
            period,
            period.firstDay(today),
            period.lastDay(today),
            sumAmounts(invoices),
            invoices.size(),
            channels,
            ZonedDateTime.now(clock).withZoneSameInstant(ReportPeriod.ZONE));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ReportPeriod.ZONE));
    }

    // Del primer día a las 00:00 hasta el día siguiente al último a las 00:00 (excluido).
    private List<InvoiceEntity> findInvoices(ReportPeriod period, LocalDate today) {
        Instant from = period.firstDay(today).atStartOfDay(ReportPeriod.ZONE).toInstant();
        Instant to = period.lastDay(today).plusDays(1).atStartOfDay(ReportPeriod.ZONE).toInstant();
        return salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, to);
    }

    private BigDecimal sumAmounts(List<InvoiceEntity> invoices) {
        return invoices.stream()
            .map(InvoiceEntity::getAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}