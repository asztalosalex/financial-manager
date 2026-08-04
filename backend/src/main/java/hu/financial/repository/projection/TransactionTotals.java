package hu.financial.repository.projection;

import java.math.BigDecimal;

public record TransactionTotals(
        BigDecimal currentIncome,
        BigDecimal currentExpense,
        BigDecimal previousIncome,
        BigDecimal previousExpense,
        BigDecimal incomeUntilCurrentEnd,
        BigDecimal expenseUntilCurrentEnd,
        BigDecimal incomeUntilPreviousEnd,
        BigDecimal expenseUntilPreviousEnd) {

    public TransactionTotals {
        currentIncome = zeroIfNull(currentIncome);
        currentExpense = zeroIfNull(currentExpense);
        previousIncome = zeroIfNull(previousIncome);
        previousExpense = zeroIfNull(previousExpense);
        incomeUntilCurrentEnd = zeroIfNull(incomeUntilCurrentEnd);
        expenseUntilCurrentEnd = zeroIfNull(expenseUntilCurrentEnd);
        incomeUntilPreviousEnd = zeroIfNull(incomeUntilPreviousEnd);
        expenseUntilPreviousEnd = zeroIfNull(expenseUntilPreviousEnd);
    }

    public BigDecimal currentBalance() {
        return incomeUntilCurrentEnd.subtract(expenseUntilCurrentEnd);
    }

    public BigDecimal previousBalance() {
        return incomeUntilPreviousEnd.subtract(expenseUntilPreviousEnd);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
