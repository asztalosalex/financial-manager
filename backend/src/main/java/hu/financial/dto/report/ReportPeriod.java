package hu.financial.dto.report;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import hu.financial.exception.InvalidRequestParameterException;

public record ReportPeriod(YearMonth month) {

    public static final String MONTH_PARAMETER = "month";

    public ReportPeriod {
        requirePrecedingMonthExists(month);
    }

    public static ReportPeriod of(String month) {
        return of(month, YearMonth.now());
    }

    public static ReportPeriod of(String month, YearMonth serverMonth) {
        return new ReportPeriod(month == null ? serverMonth : parse(month));
    }

    public YearMonth previousMonth() {
        return month.minusMonths(1);
    }

    public LocalDate start() {
        return month.atDay(1);
    }

    public LocalDate end() {
        return month.atEndOfMonth();
    }

    public LocalDate previousStart() {
        return previousMonth().atDay(1);
    }

    public LocalDate previousEnd() {
        return previousMonth().atEndOfMonth();
    }

    private static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            throw new InvalidRequestParameterException(MONTH_PARAMETER, "must be a calendar month in YYYY-MM format");
        }
    }

    private static void requirePrecedingMonthExists(YearMonth month) {
        if (month.getYear() == Year.MIN_VALUE && month.getMonthValue() == 1) {
            throw new InvalidRequestParameterException(MONTH_PARAMETER,
                    "must be a calendar month that has a preceding month");
        }
    }
}
