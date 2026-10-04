package dev.team1.kpi;

import java.time.Clock;

import org.springframework.stereotype.Service;

import dev.team1.contracts.ISalesKpiService;
import dev.team1.kpi.dtos.SalesKpiDTOResponse;
import dev.team1.reports.SalesReportRepository;

// Calcula los KPI de ventas del panel de administración a partir de las facturas pagadas.
// TDD (rojo): de momento solo existe la forma; los tests describen lo que debe calcular.
@Service
public class SalesKpiService implements ISalesKpiService {

    private final SalesReportRepository salesReportRepository;
    private final Clock clock;

    public SalesKpiService(SalesReportRepository salesReportRepository, Clock clock) {
        this.salesReportRepository = salesReportRepository;
        this.clock = clock;
    }

    @Override
    public SalesKpiDTOResponse getSalesKpi() {
        throw new UnsupportedOperationException("KPI de ventas pendiente de implementar");
    }
}