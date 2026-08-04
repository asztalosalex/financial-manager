package hu.financial.dto.budget;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import hu.financial.exception.InvalidRequestParameterException;

public record BudgetFilter(YearMonth month, Long categoryId) {

    public static final String MONTH_PARAMETER = "month";

    public static BudgetFilter of(String month, Long categoryId) {
        return new BudgetFilter(parseMonth(month), categoryId);
    }

    public static BudgetFilter unfiltered() {
        return new BudgetFilter(null, null);
    }

    private static YearMonth parseMonth(String month) {
        if (month == null) {
            return null;
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            throw new InvalidRequestParameterException(MONTH_PARAMETER, "must be a calendar month in YYYY-MM format");
        }
    }
}
