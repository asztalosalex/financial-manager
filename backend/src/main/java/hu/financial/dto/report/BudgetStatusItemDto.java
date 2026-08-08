package hu.financial.dto.report;

import java.math.BigDecimal;

public record BudgetStatusItemDto(
        Long categoryId,
        String categoryName,
        BigDecimal budgeted,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentageUsed) {
}
