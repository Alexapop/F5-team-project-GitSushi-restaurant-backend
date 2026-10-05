package dev.team1.reports;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.team1.enums.OrderChannel;
import dev.team1.reports.dtos.ChannelSales;
import dev.team1.reports.dtos.SalesReport;

// Comprueba que el generador devuelve un PDF válido, con ventas o sin ellas.
class SalesReportPdfGeneratorTest {

    private static final String PDF_HEADER = "%PDF-";
    private static final ZonedDateTime GENERATED_AT =
        ZonedDateTime.of(2026, 10, 7, 15, 0, 0, 0, ReportPeriod.ZONE);

    private final SalesReportPdfGenerator generator = new SalesReportPdfGenerator();

    @Test
    void generate_withSales_returnsPdf() {
        SalesReport report = new SalesReport(
            ReportPeriod.WEEK,
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 11),
            new BigDecimal("45.50"),
            3,
            List.of(
                new ChannelSales(OrderChannel.ONSITE, 2, new BigDecimal("27.50")),
                new ChannelSales(OrderChannel.ONLINE, 1, new BigDecimal("18.00"))),
            GENERATED_AT);

        byte[] pdf = generator.generate(report);

        assertTrue(isPdf(pdf));
    }

    @Test
    void generate_withoutSales_returnsPdf() {
        SalesReport report = new SalesReport(
            ReportPeriod.DAY,
            LocalDate.of(2026, 10, 7),
            LocalDate.of(2026, 10, 7),
            BigDecimal.ZERO,
            0,
            List.of(
                new ChannelSales(OrderChannel.ONSITE, 0, BigDecimal.ZERO),
                new ChannelSales(OrderChannel.ONLINE, 0, BigDecimal.ZERO)),
            GENERATED_AT);

        byte[] pdf = generator.generate(report);

        assertTrue(isPdf(pdf));
    }

    private boolean isPdf(byte[] bytes) {
        return bytes.length > PDF_HEADER.length()
            && new String(bytes, 0, PDF_HEADER.length(), StandardCharsets.US_ASCII).equals(PDF_HEADER);
    }
}