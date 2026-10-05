package dev.team1.automation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.automation.dtos.CronStatusDTOResponse;

// Estado y ejecución manual de la subida automática del resumen de ventas (solo admin).
@RestController
@RequestMapping(path = "${api-endpoint}/sistema")
public class CloudAutomationController {

    private final CloudAutomationService cloudAutomationService;

    public CloudAutomationController(CloudAutomationService cloudAutomationService) {
        this.cloudAutomationService = cloudAutomationService;
    }

    // Tarjeta "Cloud Automation Service" del panel de administración.
    @GetMapping("/cron-status")
    public ResponseEntity<CronStatusDTOResponse> getCronStatus() {
        return ResponseEntity.ok(cloudAutomationService.getStatus());
    }

    // Lanza la subida ahora mismo, sin esperar a la noche (para la demo o para reintentar).
    @PostMapping("/cron-run")
    public ResponseEntity<CronStatusDTOResponse> runNow() {
        cloudAutomationService.uploadDailySalesReport();
        return ResponseEntity.ok(cloudAutomationService.getStatus());
    }
}