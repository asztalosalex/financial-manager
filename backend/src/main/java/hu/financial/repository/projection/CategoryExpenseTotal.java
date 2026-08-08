package hu.financial.repository.projection;

import java.math.BigDecimal;

public record CategoryExpenseTotal(Long categoryId, String categoryName, BigDecimal total) {

    public CategoryExpenseTotal {
        total = total == null ? BigDecimal.ZERO : total;
    }
}
