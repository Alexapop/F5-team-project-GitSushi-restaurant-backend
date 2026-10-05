package dev.team1.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.team1.enums.OrderChannel;
import dev.team1.invoices.InvoiceEntity;
import dev.team1.orders.OrderEntity;
import dev.team1.reports.dtos.ChannelSales;
import dev.team1.reports.dtos.SalesReport;
import dev.team1.reports.dtos.SalesSummaryDTOResponse;

// Comprueba el rango de fechas que se consulta y los totales que se calculan.
@ExtendWith(MockitoExtension.class)
class SalesReportServiceTest {

    // Miércoles 7 de octubre de 2026, 15:00 en Madrid (13:00 UTC)
    private static final Instant NOW = Instant.parse("2026-10-07T13:00:00Z");

    @Mock
    private SalesReportRepository salesReportRepository;

    private SalesReportService salesReportService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        salesReportService = new SalesReportService(salesReportRepository, fixedClock);
    }

    @Test
    void getSummary_day_queriesFromMidnightToMidnightInMadrid() {
        Instant from = Instant.parse("2026-10-06T22:00:00Z");
        Instant to = Instant.parse("2026-10-07T22:00:00Z");
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, to))
            .thenReturn(List.of(invoice("20.00", OrderChannel.ONSITE)));

        SalesSummaryDTOResponse summary = salesReportService.getSummary(ReportPeriod.DAY);

        assertEquals(new BigDecimal("20.00"), summary.revenue());
        assertEquals(1, summary.orders());
    }

    @Test
    void getSummary_week_sumsPaidInvoicesFromMondayToSunday() {
        Instant from = Instant.parse("2026-10-04T22:00:00Z");
        Instant to = Instant.parse("2026-10-11T22:00:00Z");
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, to))
            .thenReturn(weekInvoices());

        SalesSummaryDTOResponse summary = salesReportService.getSummary(ReportPeriod.WEEK);

        assertEquals(new BigDecimal("45.50"), summary.revenue());
        assertEquals(3, summary.orders());
    }

    @Test
    void getSummary_month_endsAtMidnightAfterDaylightSavingChange() {
        // 1 de octubre 00:00 en verano (UTC+2) y 1 de noviembre 00:00 en invierno (UTC+1)
        Instant from = Instant.parse("2026-09-30T22:00:00Z");
        Instant to = Instant.parse("2026-10-31T23:00:00Z");
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, to))
            .thenReturn(List.of());

        SalesSummaryDTOResponse summary = salesReportService.getSummary(ReportPeriod.MONTH);

        assertEquals(BigDecimal.ZERO, summary.revenue());
        assertEquals(0, summary.orders());
    }

    @Test
    void getReport_week_returnsDatesTotalsChannelsAndAverageTicket() {
        Instant from = Instant.parse("2026-10-04T22:00:00Z");
        Instant to = Instant.parse("2026-10-11T22:00:00Z");
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, to))
            .thenReturn(weekInvoices());

        SalesReport report = salesReportService.getReport(ReportPeriod.WEEK);

        assertEquals(ReportPeriod.WEEK, report.period());
        assertEquals(LocalDate.of(2026, 10, 5), report.firstDay());
        assertEquals(LocalDate.of(2026, 10, 11), report.lastDay());
        assertEquals(new BigDecimal("45.50"), report.revenue());
        assertEquals(3, report.orders());
        assertEquals(new BigDecimal("15.17"), report.averageTicket());
        assertEquals(
            List.of(
                new ChannelSales(OrderChannel.ONSITE, 2, new BigDecimal("27.50")),
                new ChannelSales(OrderChannel.ONLINE, 1, new BigDecimal("18.00"))),
            report.channels());
        assertEquals(
            ZonedDateTime.of(2026, 10, 7, 15, 0, 0, 0, ReportPeriod.ZONE),
            report.generatedAt());
    }

    @Test
    void getReport_withoutInvoices_returnsZeroAverageTicket() {
        Instant from = Instant.parse("2026-10-06T22:00:00Z");
        Instant to = Instant.parse("2026-10-07T22:00:00Z");
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, to))
            .thenReturn(List.of());

        SalesReport report = salesReportService.getReport(ReportPeriod.DAY);

        assertEquals(0, report.orders());
        assertEquals(BigDecimal.ZERO, report.averageTicket());
        assertEquals(0, report.channels().get(0).orders());
        assertEquals(0, report.channels().get(1).orders());
    }

    // 12.50 + 15.00 en sala y 18.00 a domicilio = 45.50 en 3 pedidos
    private List<InvoiceEntity> weekInvoices() {
        return List.of(
            invoice("12.50", OrderChannel.ONSITE),
            invoice("18.00", OrderChannel.ONLINE),
            invoice("15.00", OrderChannel.ONSITE));
    }

    private InvoiceEntity invoice(String amount, OrderChannel channel) {
        OrderEntity order = new OrderEntity();
        order.setChannel(channel);
        InvoiceEntity invoice = new InvoiceEntity(new BigDecimal(amount), NOW);
        invoice.setOrder(order);
        return invoice;
    }
}