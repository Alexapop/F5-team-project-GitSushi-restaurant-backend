package dev.team1.reports;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.team1.contracts.ISalesReportService;
import dev.team1.reports.dtos.SalesReport;
import dev.team1.reports.dtos.SalesSummaryDTOResponse;
import dev.team1.security.JwtFilter;
import dev.team1.security.SecurityConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

// Comprueba los endpoints del resumen de ventas: JSON, PDF, permisos y periodo no válido.
@WebMvcTest(controllers = ReportController.class)
@Import(SecurityConfiguration.class)
class ReportControllerTest {

    private static final String SALES_URL = "/api/v1/reports/sales";
    private static final String SALES_PDF_URL = "/api/v1/reports/sales.pdf";
    private static final byte[] PDF_BYTES = "%PDF-1.4 test".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ISalesReportService salesReportService;

    @MockitoBean
    private SalesReportPdfGenerator pdfGenerator;

    @MockitoBean
    private JwtFilter jwtFilter;

    @BeforeEach
    void bypassJwtFilter() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void salesSummary_withPeriod_returnsTotals() throws Exception {
        when(salesReportService.getSummary(ReportPeriod.WEEK))
            .thenReturn(new SalesSummaryDTOResponse(new BigDecimal("45.50"), 3));

        mockMvc.perform(get(SALES_URL).param("period", "week"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revenue").value(45.5))
            .andExpect(jsonPath("$.orders").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void salesSummary_withoutPeriod_usesDay() throws Exception {
        when(salesReportService.getSummary(ReportPeriod.DAY))
            .thenReturn(new SalesSummaryDTOResponse(BigDecimal.ZERO, 0));

        mockMvc.perform(get(SALES_URL))
            .andExpect(status().isOk());

        verify(salesReportService).getSummary(ReportPeriod.DAY);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void salesSummary_invalidPeriod_returns400() throws Exception {
        mockMvc.perform(get(SALES_URL).param("period", "year"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(salesReportService);
    }

    @Test
    @WithMockUser(roles = "COOK")
    void salesSummary_notAdmin_returns403() throws Exception {
        mockMvc.perform(get(SALES_URL).param("period", "day"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(salesReportService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void salesReportPdf_returnsPdfAsAttachment() throws Exception {
        SalesReport report = new SalesReport(
            ReportPeriod.MONTH,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31),
            new BigDecimal("45.50"),
            3,
            List.of(),
            ZonedDateTime.of(2026, 10, 7, 15, 0, 0, 0, ReportPeriod.ZONE));
        when(salesReportService.getReport(ReportPeriod.MONTH)).thenReturn(report);
        when(pdfGenerator.generate(report)).thenReturn(PDF_BYTES);

        mockMvc.perform(get(SALES_PDF_URL).param("period", "month"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"resumen-ventas-month-2026-10-07.pdf\""))
            .andExpect(content().bytes(PDF_BYTES));
    }

    @Test
    @WithMockUser(roles = "COOK")
    void salesReportPdf_notAdmin_returns403() throws Exception {
        mockMvc.perform(get(SALES_PDF_URL).param("period", "day"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(salesReportService, pdfGenerator);
    }
}