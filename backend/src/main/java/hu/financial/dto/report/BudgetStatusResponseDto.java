package hu.financial.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record BudgetStatusResponseDto(
        String month,
        BigDecimal totalBudgeted,
        BigDecimal totalSpent,
        BigDecimal unbudgetedSpending,
        List<BudgetStatusItemDto> categories) {
}
