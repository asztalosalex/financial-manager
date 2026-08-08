package hu.financial.repository.projection;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyTotals(Integer year, Integer month, BigDecimal income, BigDecimal expense) {

    public MonthlyTotals {
        income = zeroIfNull(income);
        expense = zeroIfNull(expense);
    }

    public YearMonth yearMonth() {
        return YearMonth.of(year, month);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
