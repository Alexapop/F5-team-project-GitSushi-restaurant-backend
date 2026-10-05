package dev.team1.automation;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dev.team1.automation.dtos.CronStatusDTOResponse;
import dev.team1.contracts.ICloudStorage;
import dev.team1.contracts.ISalesReportService;
import dev.team1.reports.ReportPeriod;
import dev.team1.reports.SalesReportPdfGenerator;
import dev.team1.reports.dtos.SalesReport;

// Sube cada noche el resumen de ventas del día en PDF a la nube y guarda cómo fue
// la última sincronización para el panel del administrador.
@Service
public class CloudAutomationService {

    private static final Logger logger = LoggerFactory.getLogger(CloudAutomationService.class);

    private static final String STATUS_ONLINE = "ONLINE";
    private static final String STATUS_ERROR = "ERROR";
    private static final int MAX_ATTEMPTS = 3;
    private static final String REPORTS_FOLDER = "resumenes-ventas/";
    private static final String NOT_CONFIGURED_ERROR = "El almacenamiento en la nube no está configurado.";

    private final ISalesReportService salesReportService;
    private final SalesReportPdfGenerator pdfGenerator;
    private final ICloudStorage cloudStorage;
    private final Clock clock;

    // Estado en memoria de la última ejecución (se reinicia al arrancar el backend).
    private volatile Instant lastSyncAt;
    private volatile String lastError;

    public CloudAutomationService(ISalesReportService salesReportService,
            SalesReportPdfGenerator pdfGenerator,
            ICloudStorage cloudStorage,
            Clock clock) {
        this.salesReportService = salesReportService;
        this.pdfGenerator = pdfGenerator;
        this.cloudStorage = cloudStorage;
        this.clock = clock;
    }

    // Todas las noches a las 23:55, hora de España, con el día ya prácticamente cerrado.
    @Scheduled(cron = "${sales-report.cron:0 55 23 * * *}", zone = "Europe/Madrid")
    public void uploadDailySalesReport() {
        if (!cloudStorage.isConfigured()) {
            lastError = NOT_CONFIGURED_ERROR;
            logger.warn("Resumen diario de ventas no subido: {}", NOT_CONFIGURED_ERROR);
            return;
        }

        SalesReport report;
        byte[] pdf;
        try {
            report = salesReportService.getReport(ReportPeriod.DAY);
            pdf = pdfGenerator.generate(report);
        } catch (RuntimeException exception) {
            registerFailure("No se pudo generar el resumen de ventas: " + exception.getMessage(), exception);
            return;
        }

        uploadWithRetries(REPORTS_FOLDER + "resumen-ventas-" + report.firstDay() + ".pdf", pdf);
    }

    public CronStatusDTOResponse getStatus() {
        if (!cloudStorage.isConfigured()) {
            return new CronStatusDTOResponse(STATUS_ERROR, lastSyncAt, NOT_CONFIGURED_ERROR);
        }
        String status = lastError == null ? STATUS_ONLINE : STATUS_ERROR;
        return new CronStatusDTOResponse(status, lastSyncAt, lastError);
    }

    private void uploadWithRetries(String path, byte[] pdf) {
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                cloudStorage.upload(path, pdf);
                lastSyncAt = Instant.now(clock);
                lastError = null;
                logger.info("Resumen diario de ventas subido a la nube: {}", path);
                return;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                lastFailure = exception;
                break;
            } catch (IOException | RuntimeException exception) {
                lastFailure = exception;
                logger.warn("Fallo al subir {} (intento {}/{}): {}",
                    path, attempt, MAX_ATTEMPTS, exception.getMessage());
            }
        }

        registerFailure("No se pudo subir el resumen tras " + MAX_ATTEMPTS + " intentos: "
            + lastFailure.getMessage(), lastFailure);
    }

    private void registerFailure(String message, Exception cause) {
        lastError = message;
        logger.error(message, cause);
    }
}