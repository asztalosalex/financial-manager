package hu.financial.dto.report;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

import hu.financial.exception.InvalidRequestParameterException;

public record TrendPeriod(YearMonth month, int months) {

    public static final String MONTHS_PARAMETER = "months";

    public static final int DEFAULT_MONTHS = 6;

    public static final int MIN_MONTHS = 1;

    public static final int MAX_MONTHS = 24;

    public TrendPeriod {
        requireSupportedLength(months);
        requireRepresentableRange(month, months);
    }

    public static TrendPeriod of(String month, Integer months) {
        return of(month, months, YearMonth.now());
    }

    public static TrendPeriod of(String month, Integer months, YearMonth serverMonth) {
        return new TrendPeriod(
                ReportPeriod.parseMonth(month, serverMonth),
                months == null ? DEFAULT_MONTHS : months);
    }

    public YearMonth firstMonth() {
        return month.minusMonths(months - 1L);
    }

    public LocalDate start() {
        return firstMonth().atDay(1);
    }

    public LocalDate end() {
        return month.atEndOfMonth();
    }

    public List<YearMonth> monthsInOrder() {
        YearMonth first = firstMonth();
        return IntStream.range(0, months).mapToObj(offset -> first.plusMonths(offset)).toList();
    }

    private static void requireSupportedLength(int months) {
        if (months < MIN_MONTHS || months > MAX_MONTHS) {
            throw new InvalidRequestParameterException(MONTHS_PARAMETER,
                    "must be an integer between " + MIN_MONTHS + " and " + MAX_MONTHS);
        }
    }

    private static void requireRepresentableRange(YearMonth month, int months) {
        try {
            month.minusMonths(months - 1L);
        } catch (DateTimeException ex) {
            throw new InvalidRequestParameterException(ReportPeriod.MONTH_PARAMETER,
                    "must be a calendar month preceded by " + (months - 1) + " representable months");
        }
    }
}
