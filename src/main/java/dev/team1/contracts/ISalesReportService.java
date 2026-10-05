package dev.team1.contracts;

import dev.team1.reports.ReportPeriod;
import dev.team1.reports.dtos.SalesReport;
import dev.team1.reports.dtos.SalesSummaryDTOResponse;

public interface ISalesReportService {
    SalesSummaryDTOResponse getSummary(ReportPeriod period);
    SalesReport getReport(ReportPeriod period);
}