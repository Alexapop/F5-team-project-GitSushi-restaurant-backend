package dev.team1.kpi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.team1.contracts.ISalesKpiService;
import dev.team1.enums.OrderChannel;
import dev.team1.invoices.InvoiceEntity;
import dev.team1.kpi.dtos.ChannelShare;
import dev.team1.kpi.dtos.ChannelSplit;
import dev.team1.kpi.dtos.DailySales;
import dev.team1.kpi.dtos.PeriodSales;
import dev.team1.kpi.dtos.SalesKpiDTOResponse;
import dev.team1.reports.ReportPeriod;
import dev.team1.reports.SalesReportRepository;

// Calcula los KPI de ventas del panel de administración a partir de las facturas pagadas.
// Las fechas van en hora de España. Cada periodo llega hasta hoy incluido y se compara
// con el mismo tramo del periodo anterior (p. ej. del 1 al 7 de este mes con el 1 al 7 del pasado).
@Service
public class SalesKpiService implements ISalesKpiService {

    private static final Period DAY = Period.ofDays(1);
    private static final Period MONTH = Period.ofMonths(1);
    private static final Period QUARTER = Period.ofMonths(3);
    private static final Period YEAR = Period.ofYears(1);
    private static final int MONTHS_PER_QUARTER = 3;
    private static final int DAYS_PER_WEEK = 7;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final SalesReportRepository salesReportRepository;
    private final Clock clock;

    public SalesKpiService(SalesReportRepository salesReportRepository, Clock clock) {
        this.salesReportRepository = salesReportRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public SalesKpiDTOResponse getSalesKpi() {
        LocalDate today = LocalDate.now(clock.withZone(ReportPeriod.ZONE));
        LocalDate tomorrow = today.plusDays(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate quarterStart = firstDayOfQuarter(today);
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Una sola consulta: desde el 1 de enero del año pasado (lo más antiguo que se compara) hasta hoy.
        List<PaidSale> sales = findPaidSales(yearStart.minus(YEAR), tomorrow);
        List<DailySales> weekly = weeklySales(sales, weekStart);

        return new SalesKpiDTOResponse(
            periodSales(sales, today, tomorrow, DAY),
            periodSales(sales, monthStart, tomorrow, MONTH),
            periodSales(sales, quarterStart, tomorrow, QUARTER),
            periodSales(sales, yearStart, tomorrow, YEAR),
            channelSplit(between(sales, monthStart, tomorrow)),
            weekly,
            peakDay(weekly));
    }

    private List<PaidSale> findPaidSales(LocalDate from, LocalDate toExclusive) {
        return salesReportRepository
            .findByPaidAtGreaterThanEqualAndPaidAtLessThan(startOf(from), startOf(toExclusive))
            .stream()
            .map(PaidSale::from)
            .toList();
    }

    private PeriodSales periodSales(List<PaidSale> sales, LocalDate start, LocalDate endExclusive, Period length) {
        return new PeriodSales(
            sum(between(sales, start, endExclusive)),
            sum(between(sales, start.minus(length), endExclusive.minus(length))));
    }

    private ChannelSplit channelSplit(List<PaidSale> sales) {
        BigDecimal inStore = sum(ofChannel(sales, OrderChannel.ONSITE));
        BigDecimal delivery = sum(ofChannel(sales, OrderChannel.ONLINE));
        BigDecimal total = inStore.add(delivery);

        if (total.signum() == 0) {
            return new ChannelSplit(new ChannelShare(inStore, 0), new ChannelShare(delivery, 0));
        }

        // El segundo porcentaje es lo que falta hasta 100, para que la barra siempre sume 100 %.
        int inStorePercentage = inStore.multiply(ONE_HUNDRED)
            .divide(total, 0, RoundingMode.HALF_UP)
            .intValue();
        return new ChannelSplit(
            new ChannelShare(inStore, inStorePercentage),
            new ChannelShare(delivery, 100 - inStorePercentage));
    }

    private List<DailySales> weeklySales(List<PaidSale> sales, LocalDate weekStart) {
        return IntStream.range(0, DAYS_PER_WEEK)
            .mapToObj(weekStart::plusDays)
            .map(day -> {
                List<PaidSale> daySales = between(sales, day, day.plusDays(1));
                return new DailySales(
                    day.getDayOfWeek(),
                    sum(ofChannel(daySales, OrderChannel.ONSITE)),
                    sum(ofChannel(daySales, OrderChannel.ONLINE)));
            })
            .toList();
    }

    // Día con más ventas de la semana; null si todavía no se ha vendido nada.
    private DayOfWeek peakDay(List<DailySales> weekly) {
        return weekly.stream()
            .filter(day -> dayTotal(day).signum() > 0)
            .max(Comparator.comparing(this::dayTotal))
            .map(DailySales::day)
            .orElse(null);
    }

    private BigDecimal dayTotal(DailySales day) {
        return day.inStore().add(day.delivery());
    }

    private List<PaidSale> between(List<PaidSale> sales, LocalDate from, LocalDate toExclusive) {
        return sales.stream()
            .filter(sale -> !sale.day().isBefore(from) && sale.day().isBefore(toExclusive))
            .toList();
    }

    private List<PaidSale> ofChannel(List<PaidSale> sales, OrderChannel channel) {
        return sales.stream().filter(sale -> sale.channel() == channel).toList();
    }

    private BigDecimal sum(List<PaidSale> sales) {
        return sales.stream().map(PaidSale::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate firstDayOfQuarter(LocalDate date) {
        int firstMonth = ((date.getMonthValue() - 1) / MONTHS_PER_QUARTER) * MONTHS_PER_QUARTER + 1;
        return LocalDate.of(date.getYear(), firstMonth, 1);
    }

    private Instant startOf(LocalDate day) {
        return day.atStartOfDay(ReportPeriod.ZONE).toInstant();
    }

    // Lo único que hace falta de cada factura: el día (en hora de España), el canal y el importe.
    private record PaidSale(LocalDate day, OrderChannel channel, BigDecimal amount) {

        static PaidSale from(InvoiceEntity invoice) {
            OrderChannel channel = invoice.getOrder() != null ? invoice.getOrder().getChannel() : null;
            BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
            return new PaidSale(LocalDate.ofInstant(invoice.getPaidAt(), ReportPeriod.ZONE), channel, amount);
        }
    }
}