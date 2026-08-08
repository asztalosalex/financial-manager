package hu.financial.repository.projection;

import java.math.BigDecimal;

public record CategoryBudgetTotal(Long categoryId, String categoryName, BigDecimal total) {

    public CategoryBudgetTotal {
        total = total == null ? BigDecimal.ZERO : total;
    }
}
