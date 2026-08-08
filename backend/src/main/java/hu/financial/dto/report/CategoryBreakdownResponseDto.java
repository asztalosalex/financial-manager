package hu.financial.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record CategoryBreakdownResponseDto(
        String month,
        BigDecimal total,
        List<CategoryBreakdownItemDto> categories) {
}
