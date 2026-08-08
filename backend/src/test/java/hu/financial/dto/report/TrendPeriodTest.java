package hu.financial.dto.report;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import hu.financial.exception.InvalidRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrendPeriodTest {

    private static final YearMonth SERVER_MONTH = YearMonth.of(2026, 8);

    @Test
    void of_ParsesTheLastMonthOfTheSeries() {
        TrendPeriod period = TrendPeriod.of("2026-07", 6, SERVER_MONTH);

        assertEquals(YearMonth.of(2026, 7), period.month());
        assertEquals(6, period.months());
    }

    @Test
    void of_WithoutMonth_FallsBackToTheServerMonth() {
        TrendPeriod period = TrendPeriod.of(null, 6, SERVER_MONTH);

        assertEquals(SERVER_MONTH, period.month());
    }

    @Test
    void of_WithoutMonths_DefaultsToSix() {
        TrendPeriod period = TrendPeriod.of("2026-07", null, SERVER_MONTH);

        assertEquals(6, period.months());
        assertEquals(6, period.monthsInOrder().size());
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-07-01", "2026-7", "2026", "07-2026", "2026-13", "", "   ", "last month" })
    void of_RejectsAMonthThatIsNotYearDashMonth(String month) {
        InvalidRequestParameterException exception = assertThrows(
                InvalidRequestParameterException.class, () -> TrendPeriod.of(month, 6, SERVER_MONTH));

        assertEquals("month", exception.getParameter());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, 25, 100, Integer.MIN_VALUE, Integer.MAX_VALUE })
    void of_RejectsALengthOutsideOneToTwentyFour(int months) {
        InvalidRequestParameterException exception = assertThrows(
                InvalidRequestParameterException.class, () -> TrendPeriod.of("2026-07", months, SERVER_MONTH));

        assertEquals("months", exception.getParameter());
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 6, 12, 23, 24 })
    void of_AcceptsEveryLengthInsideTheSupportedRange(int months) {
        TrendPeriod period = TrendPeriod.of("2026-07", months, SERVER_MONTH);

        assertEquals(months, period.months());
        assertEquals(months, period.monthsInOrder().size());
    }

    @Test
    void monthsInOrder_HasExactlyMonthsEntriesEndingWithTheRequestedMonth() {
        TrendPeriod period = TrendPeriod.of("2026-08", 6, SERVER_MONTH);

        assertEquals(List.of(
                YearMonth.of(2026, 3),
                YearMonth.of(2026, 4),
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 6),
                YearMonth.of(2026, 7),
                YearMonth.of(2026, 8)), period.monthsInOrder());
    }

    @Test
    void monthsInOrder_IsChronologicallyAscending_SoTheChartDrawsLeftToRight() {
        List<YearMonth> months = TrendPeriod.of("2026-08", 4, SERVER_MONTH).monthsInOrder();

        assertEquals(YearMonth.of(2026, 5), months.get(0));
        assertEquals(YearMonth.of(2026, 8), months.get(months.size() - 1));
    }

    @Test
    void monthsInOrder_CrossesTheYearBoundary() {
        TrendPeriod period = TrendPeriod.of("2026-02", 4, SERVER_MONTH);

        assertEquals(List.of(
                YearMonth.of(2025, 11),
                YearMonth.of(2025, 12),
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2)), period.monthsInOrder());
    }

    @Test
    void monthsInOrder_ForASingleMonthSeries_IsJustTheRequestedMonth() {
        TrendPeriod period = TrendPeriod.of("2026-07", 1, SERVER_MONTH);

        assertEquals(List.of(YearMonth.of(2026, 7)), period.monthsInOrder());
        assertEquals(YearMonth.of(2026, 7), period.firstMonth());
    }

    @Test
    void boundaries_SpanFromTheFirstDayOfTheFirstMonthToTheLastDayOfTheRequestedMonth() {
        TrendPeriod period = TrendPeriod.of("2026-08", 6, SERVER_MONTH);

        assertEquals(LocalDate.of(2026, 3, 1), period.start());
        assertEquals(LocalDate.of(2026, 8, 31), period.end());
    }

    @Test
    void end_KnowsTheLeapDay() {
        assertEquals(LocalDate.of(2024, 2, 29), TrendPeriod.of("2024-02", 3, SERVER_MONTH).end());
    }

    @Test
    void aFutureMonth_IsAcceptedBecauseItIsNotAnError() {
        TrendPeriod period = TrendPeriod.of("2099-12", 3, SERVER_MONTH);

        assertEquals(YearMonth.of(2099, 12), period.month());
        assertEquals(YearMonth.of(2099, 10), period.firstMonth());
    }

    @Test
    void of_RejectsAMonthWhoseSeriesWouldReachBeforeTheFirstRepresentableMonth() {
        InvalidRequestParameterException exception = assertThrows(
                InvalidRequestParameterException.class,
                () -> TrendPeriod.of("-999999999-02", 24, SERVER_MONTH));

        assertEquals("month", exception.getParameter());
    }
}
