package hu.financial.dto.report;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import hu.financial.exception.InvalidRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportPeriodTest {

    private static final YearMonth SERVER_MONTH = YearMonth.of(2026, 8);

    @Test
    void of_ParsesACalendarMonth() {
        ReportPeriod period = ReportPeriod.of("2026-02", SERVER_MONTH);

        assertEquals(YearMonth.of(2026, 2), period.month());
    }

    @Test
    void of_WithoutMonth_FallsBackToTheServerMonth() {
        ReportPeriod period = ReportPeriod.of(null, SERVER_MONTH);

        assertEquals(SERVER_MONTH, period.month());
    }

    @Test
    void of_WithoutArgumentsAtAll_UsesTheServerCalendarMonth() {
        ReportPeriod period = ReportPeriod.of(null);

        assertEquals(YearMonth.now(), period.month());
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-02-01", "2026-2", "2026", "02-2026", "2026-13", "", "   ", "last month" })
    void of_RejectsAnythingThatIsNotYearDashMonth(String month) {
        InvalidRequestParameterException exception = assertThrows(
                InvalidRequestParameterException.class, () -> ReportPeriod.of(month, SERVER_MONTH));

        assertEquals("month", exception.getParameter());
    }

    @Test
    void previousMonth_IsTheImmediatelyPrecedingCalendarMonth() {
        assertEquals(YearMonth.of(2026, 7), ReportPeriod.of("2026-08", SERVER_MONTH).previousMonth());
    }

    @Test
    void previousMonth_CrossesTheYearBoundary() {
        assertEquals(YearMonth.of(2025, 12), ReportPeriod.of("2026-01", SERVER_MONTH).previousMonth());
    }

    @Test
    void boundaries_SpanTheWholeRequestedMonthInclusively() {
        ReportPeriod period = ReportPeriod.of("2026-07", SERVER_MONTH);

        assertEquals(LocalDate.of(2026, 7, 1), period.start());
        assertEquals(LocalDate.of(2026, 7, 31), period.end());
    }

    @Test
    void previousBoundaries_SpanTheWholePrecedingMonthInclusively() {
        ReportPeriod period = ReportPeriod.of("2026-07", SERVER_MONTH);

        assertEquals(LocalDate.of(2026, 6, 1), period.previousStart());
        assertEquals(LocalDate.of(2026, 6, 30), period.previousEnd());
    }

    @Test
    void end_KnowsTheLeapDay() {
        assertEquals(LocalDate.of(2024, 2, 29), ReportPeriod.of("2024-02", SERVER_MONTH).end());
    }

    @Test
    void previousEnd_IsTheDayBeforeTheRequestedMonthStarts() {
        ReportPeriod period = ReportPeriod.of("2026-03", SERVER_MONTH);

        assertEquals(period.start().minusDays(1), period.previousEnd());
    }

    @Test
    void aFutureMonth_IsAcceptedBecauseItIsNotAnError() {
        ReportPeriod period = ReportPeriod.of("2099-12", SERVER_MONTH);

        assertEquals(YearMonth.of(2099, 12), period.month());
        assertEquals(YearMonth.of(2099, 11), period.previousMonth());
    }

    @Test
    void of_RejectsTheFirstRepresentableMonthBecauseItHasNoPredecessor() {
        InvalidRequestParameterException exception = assertThrows(
                InvalidRequestParameterException.class,
                () -> ReportPeriod.of("-999999999-01", SERVER_MONTH));

        assertEquals("month", exception.getParameter());
    }
}
