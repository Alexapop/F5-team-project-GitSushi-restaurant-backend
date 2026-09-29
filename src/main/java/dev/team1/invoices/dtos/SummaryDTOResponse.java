package dev.team1.invoices.dtos;

import java.math.BigDecimal;

public record SummaryDTOResponse(
    Summary summary
) {
  public record Summary(
      PeriodSummary total,
      PeriodSummary online,
      PeriodSummary onsite
  ) {}

  public record PeriodSummary(
      Metrics daily,
      Metrics monthly,
      Metrics trimestry,
      Metrics yearly
  ) {}

  public record Metrics(
      long orders,
      BigDecimal revenue
  ) {}
}