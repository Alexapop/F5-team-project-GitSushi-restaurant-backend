package dev.team1.reports;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.contracts.ISalesReportService;
import dev.team1.reports.dtos.SalesReport;
import dev.team1.reports.dtos.SalesSummaryDTOResponse;

// Resumen de ventas para el administrador (GS-475): totales del periodo y PDF.
@RestController
@RequestMapping(path = "${api-endpoint}/reports")
public class ReportController {

    private final ISalesReportService salesReportService;
    private final SalesReportPdfGenerator pdfGenerator;

    public ReportController(ISalesReportService salesReportService, SalesReportPdfGenerator pdfGenerator) {
        this.salesReportService = salesReportService;
        this.pdfGenerator = pdfGenerator;
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesSummaryDTOResponse> salesSummary(
        @RequestParam(defaultValue = "day") String period
    ) {
        return ResponseEntity.ok(salesReportService.getSummary(ReportPeriod.from(period)));
    }

    @GetMapping("/sales.pdf")
    public ResponseEntity<byte[]> salesReportPdf(
        @RequestParam(defaultValue = "day") String period
    ) {
        SalesReport report = salesReportService.getReport(ReportPeriod.from(period));
        String fileName = "resumen-ventas-" + report.period().getValue() + "-"
            + report.generatedAt().toLocalDate() + ".pdf";

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(fileName).build().toString())
            .body(pdfGenerator.generate(report));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidPeriod(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}