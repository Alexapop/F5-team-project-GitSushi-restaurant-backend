package dev.team1.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// Comprueba cómo se leen los periodos y qué días abarca cada uno.
class ReportPeriodTest {

    // Miércoles 7 de octubre de 2026
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);

    @Test
    void from_acceptsLowercaseUppercaseAndSpaces() {
        assertEquals(ReportPeriod.DAY, ReportPeriod.from("day"));
        assertEquals(ReportPeriod.WEEK, ReportPeriod.from("WEEK"));
        assertEquals(ReportPeriod.MONTH, ReportPeriod.from(" month "));
    }

    @Test
    void from_invalidValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ReportPeriod.from("year"));
    }

    @Test
    void from_nullOrBlank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ReportPeriod.from(null));
        assertThrows(IllegalArgumentException.class, () -> ReportPeriod.from("  "));
    }

    @Test
    void getValue_returnsLowercaseName() {
        assertEquals("day", ReportPeriod.DAY.getValue());
        assertEquals("week", ReportPeriod.WEEK.getValue());
        assertEquals("month", ReportPeriod.MONTH.getValue());
    }

    @Test
    void day_startsAndEndsToday() {
        assertEquals(WEDNESDAY, ReportPeriod.DAY.firstDay(WEDNESDAY));
        assertEquals(WEDNESDAY, ReportPeriod.DAY.lastDay(WEDNESDAY));
    }

    @Test
    void week_goesFromMondayToSunday() {
        assertEquals(LocalDate.of(2026, 10, 5), ReportPeriod.WEEK.firstDay(WEDNESDAY));
        assertEquals(LocalDate.of(2026, 10, 11), ReportPeriod.WEEK.lastDay(WEDNESDAY));
    }

    @Test
    void week_onSunday_stillStartsOnPreviousMonday() {
        LocalDate sunday = LocalDate.of(2026, 10, 11);

        assertEquals(LocalDate.of(2026, 10, 5), ReportPeriod.WEEK.firstDay(sunday));
        assertEquals(sunday, ReportPeriod.WEEK.lastDay(sunday));
    }

    @Test
    void month_goesFromFirstToLastDay() {
        assertEquals(LocalDate.of(2026, 10, 1), ReportPeriod.MONTH.firstDay(WEDNESDAY));
        assertEquals(LocalDate.of(2026, 10, 31), ReportPeriod.MONTH.lastDay(WEDNESDAY));
    }
}