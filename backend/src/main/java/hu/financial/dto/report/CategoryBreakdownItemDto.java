package hu.financial.dto.report;

import java.math.BigDecimal;

public record CategoryBreakdownItemDto(
        Long categoryId,
        String categoryName,
        BigDecimal total,
        BigDecimal percentage) {
}
