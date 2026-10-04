package dev.team1.kpi;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.contracts.ISalesKpiService;
import dev.team1.kpi.dtos.SalesKpiDTOResponse;

// KPI de ventas del panel de administración (GS-486). Solo ADMIN (ver SecurityConfiguration).
@RestController
@RequestMapping(path = "${api-endpoint}/kpi")
public class KpiController {

    private final ISalesKpiService salesKpiService;

    public KpiController(ISalesKpiService salesKpiService) {
        this.salesKpiService = salesKpiService;
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesKpiDTOResponse> salesKpi() {
        return ResponseEntity.ok(salesKpiService.getSalesKpi());
    }
}