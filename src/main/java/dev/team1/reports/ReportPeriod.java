package dev.team1.reports;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

// Periodos del resumen de ventas. El valor en minúsculas (day, week, month)
// es el que envía el front en ?period=. Las fechas se calculan en hora de España.
public enum ReportPeriod {
    DAY("Hoy"),
    WEEK("Esta semana"),
    MONTH("Este mes");

    public static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

    private final String label;

    ReportPeriod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ReportPeriod from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El periodo es obligatorio (day, week o month)");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Periodo no válido: " + value + " (usa day, week o month)");
        }
    }

    // Primer día del periodo (incluido): hoy, el lunes de esta semana o el día 1 del mes.
    public LocalDate firstDay(LocalDate today) {
        return switch (this) {
            case DAY -> today;
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> today.withDayOfMonth(1);
        };
    }

    // Último día del periodo (incluido): hoy, el domingo de esta semana o el último día del mes.
    public LocalDate lastDay(LocalDate today) {
        return switch (this) {
            case DAY -> today;
            case WEEK -> firstDay(today).plusDays(6);
            case MONTH -> today.with(TemporalAdjusters.lastDayOfMonth());
        };
    }
}