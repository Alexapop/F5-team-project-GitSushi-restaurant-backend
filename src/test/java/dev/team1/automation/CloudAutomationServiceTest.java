package dev.team1.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.team1.automation.dtos.CronStatusDTOResponse;
import dev.team1.contracts.ICloudStorage;
import dev.team1.contracts.ISalesReportService;
import dev.team1.enums.OrderChannel;
import dev.team1.reports.ReportPeriod;
import dev.team1.reports.SalesReportPdfGenerator;
import dev.team1.reports.dtos.ChannelSales;
import dev.team1.reports.dtos.SalesReport;

// Subida nocturna del resumen de ventas a la nube: PDF del día, reintentos y estado.
@ExtendWith(MockitoExtension.class)
class CloudAutomationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T21:55:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final String EXPECTED_PATH = "resumenes-ventas/resumen-ventas-2026-10-05.pdf";
    private static final String PDF_HEADER = "%PDF-";

    @Mock
    private ISalesReportService salesReportService;
    @Mock
    private ICloudStorage cloudStorage;

    private CloudAutomationService service;

    @BeforeEach
    void setUp() {
        service = new CloudAutomationService(
            salesReportService, new SalesReportPdfGenerator(), cloudStorage, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void uploadDailySalesReport_uploadsTodaysPdfAndReportsOnline() throws Exception {
        when(cloudStorage.isConfigured()).thenReturn(true);
        when(salesReportService.getReport(ReportPeriod.DAY)).thenReturn(todayReport());

        service.uploadDailySalesReport();

        ArgumentCaptor<byte[]> pdf = ArgumentCaptor.forClass(byte[].class);
        verify(cloudStorage).upload(eq(EXPECTED_PATH), pdf.capture());
        assertTrue(new String(pdf.getValue(), StandardCharsets.ISO_8859_1).startsWith(PDF_HEADER));

        CronStatusDTOResponse status = service.getStatus();
        assertEquals("ONLINE", status.status());
        assertEquals(NOW, status.lastSyncAt());
        assertNull(status.lastError());
    }

    @Test
    void uploadDailySalesReport_retriesAndSucceedsOnThirdAttempt() throws Exception {
        when(cloudStorage.isConfigured()).thenReturn(true);
        when(salesReportService.getReport(ReportPeriod.DAY)).thenReturn(todayReport());
        doThrow(new IOException("timeout"))
            .doThrow(new IOException("timeout"))
            .doNothing()
            .when(cloudStorage).upload(eq(EXPECTED_PATH), any(byte[].class));

        service.uploadDailySalesReport();

        verify(cloudStorage, times(3)).upload(eq(EXPECTED_PATH), any(byte[].class));
        assertEquals("ONLINE", service.getStatus().status());
    }

    @Test
    void uploadDailySalesReport_afterThreeFailures_reportsErrorWithReason() throws Exception {
        when(cloudStorage.isConfigured()).thenReturn(true);
        when(salesReportService.getReport(ReportPeriod.DAY)).thenReturn(todayReport());
        doThrow(new IOException("Supabase respondió HTTP 500"))
            .when(cloudStorage).upload(eq(EXPECTED_PATH), any(byte[].class));

        service.uploadDailySalesReport();

        verify(cloudStorage, times(3)).upload(eq(EXPECTED_PATH), any(byte[].class));
        CronStatusDTOResponse status = service.getStatus();
        assertEquals("ERROR", status.status());
        assertNull(status.lastSyncAt());
        assertTrue(status.lastError().contains("HTTP 500"));
    }

    @Test
    void uploadDailySalesReport_withoutCloudConfig_doesNotUploadAndReportsError() throws Exception {
        when(cloudStorage.isConfigured()).thenReturn(false);

        service.uploadDailySalesReport();

        verify(cloudStorage, never()).upload(anyString(), any(byte[].class));
        verifyNoInteractions(salesReportService);
        CronStatusDTOResponse status = service.getStatus();
        assertEquals("ERROR", status.status());
        assertTrue(status.lastError().contains("no está configurado"));
    }

    @Test
    void uploadDailySalesReport_whenItWorksAgain_clearsThePreviousError() throws Exception {
        when(cloudStorage.isConfigured()).thenReturn(true);
        when(salesReportService.getReport(ReportPeriod.DAY)).thenReturn(todayReport());
        doThrow(new IOException("caído"))
            .doThrow(new IOException("caído"))
            .doThrow(new IOException("caído"))
            .doNothing()
            .when(cloudStorage).upload(eq(EXPECTED_PATH), any(byte[].class));

        service.uploadDailySalesReport();
        assertEquals("ERROR", service.getStatus().status());

        service.uploadDailySalesReport();
        CronStatusDTOResponse status = service.getStatus();
        assertEquals("ONLINE", status.status());
        assertNull(status.lastError());
    }

    private SalesReport todayReport() {
        return new SalesReport(
            ReportPeriod.DAY,
            TODAY,
            TODAY,
            new BigDecimal("23.10"),
            1,
            List.of(new ChannelSales(OrderChannel.ONLINE, 1, new BigDecimal("23.10"))),
            ZonedDateTime.of(2026, 10, 5, 23, 55, 0, 0, ReportPeriod.ZONE));
    }
}