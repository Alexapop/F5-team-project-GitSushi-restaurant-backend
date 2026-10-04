package dev.team1.kpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.team1.enums.OrderChannel;
import dev.team1.invoices.InvoiceEntity;
import dev.team1.kpi.dtos.DailySales;
import dev.team1.kpi.dtos.PeriodSales;
import dev.team1.kpi.dtos.SalesKpiDTOResponse;
import dev.team1.orders.OrderEntity;
import dev.team1.reports.ReportPeriod;
import dev.team1.reports.SalesReportRepository;

// TDD: estos tests describen los KPI antes de implementarlos.
// "Hoy" es el miércoles 7 de octubre de 2026 a las 15:00 en Madrid.
@ExtendWith(MockitoExtension.class)
class SalesKpiServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T13:00:00Z");
    // Una sola consulta: del 1 de enero de 2025 al 8 de octubre de 2026, a medianoche en Madrid.
    private static final Instant FROM = Instant.parse("2024-12-31T23:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-07T22:00:00Z");

    @Mock
    private SalesReportRepository salesReportRepository;

    private SalesKpiService salesKpiService;

    @BeforeEach
    void setUp() {
        salesKpiService = new SalesKpiService(salesReportRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void comparesEachPeriodWithTheSameStretchOfThePreviousOne() {
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(FROM, TO))
            .thenReturn(sampleInvoices());

        SalesKpiDTOResponse kpi = salesKpiService.getSalesKpi();

        // Hoy (7 oct) frente a ayer (6 oct)
        assertPeriod(kpi.today(), "30", "15");
        // Del 1 al 7 de octubre frente al 1 al 7 de septiembre
        assertPeriod(kpi.month(), "110", "50");
        // Trimestre (desde el 1 de octubre) frente al 1 al 7 de julio
        assertPeriod(kpi.quarter(), "110", "40");
        // Año (desde el 1 de enero) frente al mismo tramo de 2025
        assertPeriod(kpi.year(), "200", "100");
    }

    @Test
    void splitsTheMonthSalesByChannelWithPercentagesThatAddUpTo100() {
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(FROM, TO))
            .thenReturn(sampleInvoices());

        SalesKpiDTOResponse kpi = salesKpiService.getSalesKpi();

        // Sala: 20 + 40 + 25 = 85 (77 %) · Domicilio: 10 + 15 = 25 (23 %)
        assertThat(kpi.channels().inStore().revenue()).isEqualByComparingTo("85");
        assertThat(kpi.channels().inStore().percentage()).isEqualTo(77);
        assertThat(kpi.channels().delivery().revenue()).isEqualByComparingTo("25");
        assertThat(kpi.channels().delivery().percentage()).isEqualTo(23);
    }

    @Test
    void returnsTheSalesOfEachDayOfTheCurrentWeekFromMondayToSunday() {
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(FROM, TO))
            .thenReturn(sampleInvoices());

        List<DailySales> weekly = salesKpiService.getSalesKpi().weekly();

        assertThat(weekly).extracting(DailySales::day).containsExactly(DayOfWeek.values());
        assertDay(weekly.get(0), "40", "0");
        assertDay(weekly.get(1), "0", "15");
        assertDay(weekly.get(2), "20", "10");
        // El domingo 4 a las 23:30 es de la semana anterior: este domingo aún no hay ventas.
        assertDay(weekly.get(6), "0", "0");
    }

    @Test
    void marksTheDayWithMostSalesAsPeakDay() {
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(FROM, TO))
            .thenReturn(sampleInvoices());

        assertThat(salesKpiService.getSalesKpi().peakDay()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void countsInvoicesWithoutOrderInTheTotalsButNotInTheChannels() {
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(FROM, TO))
            .thenReturn(List.of(invoice("12.00", null, "2026-10-07T10:00")));

        SalesKpiDTOResponse kpi = salesKpiService.getSalesKpi();

        assertThat(kpi.today().revenue()).isEqualByComparingTo("12");
        assertThat(kpi.channels().inStore().percentage()).isZero();
        assertThat(kpi.channels().delivery().percentage()).isZero();
    }

    @Test
    void returnsZerosAndNoPeakDayWhenThereAreNoSales() {
        when(salesReportRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(FROM, TO))
            .thenReturn(List.of());

        SalesKpiDTOResponse kpi = salesKpiService.getSalesKpi();

        assertPeriod(kpi.today(), "0", "0");
        assertPeriod(kpi.year(), "0", "0");
        assertThat(kpi.channels().inStore().percentage()).isZero();
        assertThat(kpi.channels().delivery().percentage()).isZero();
        assertThat(kpi.weekly()).hasSize(7);
        assertThat(kpi.peakDay()).isNull();
    }

    // Facturas de ejemplo (fecha y hora en Madrid).
    private List<InvoiceEntity> sampleInvoices() {
        return List.of(
            invoice("20.00", OrderChannel.ONSITE, "2026-10-07T12:00"),  // hoy, miércoles
            invoice("10.00", OrderChannel.ONLINE, "2026-10-07T09:00"),  // hoy, miércoles
            invoice("15.00", OrderChannel.ONLINE, "2026-10-06T21:00"),  // ayer, martes
            invoice("40.00", OrderChannel.ONSITE, "2026-10-05T13:00"),  // lunes
            invoice("25.00", OrderChannel.ONSITE, "2026-10-04T23:30"),  // domingo de la semana pasada
            invoice("50.00", OrderChannel.ONSITE, "2026-09-03T13:00"),  // mes pasado
            invoice("40.00", OrderChannel.ONLINE, "2026-07-02T13:00"),  // trimestre pasado
            invoice("100.00", OrderChannel.ONSITE, "2025-03-10T13:00")); // año pasado
    }

    private InvoiceEntity invoice(String amount, OrderChannel channel, String madridDateTime) {
        Instant paidAt = LocalDateTime.parse(madridDateTime).atZone(ReportPeriod.ZONE).toInstant();
        InvoiceEntity invoice = new InvoiceEntity(new BigDecimal(amount), paidAt);
        if (channel != null) {
            OrderEntity order = new OrderEntity();
            order.setChannel(channel);
            invoice.setOrder(order);
        }
        return invoice;
    }

    private void assertPeriod(PeriodSales period, String revenue, String previousRevenue) {
        assertThat(period.revenue()).isEqualByComparingTo(revenue);
        assertThat(period.previousRevenue()).isEqualByComparingTo(previousRevenue);
    }

    private void assertDay(DailySales day, String inStore, String delivery) {
        assertThat(day.inStore()).isEqualByComparingTo(inStore);
        assertThat(day.delivery()).isEqualByComparingTo(delivery);
    }
}