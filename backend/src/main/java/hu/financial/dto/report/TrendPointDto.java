package hu.financial.dto.report;

import java.math.BigDecimal;

public record TrendPointDto(String month, BigDecimal income, BigDecimal expense) {
}
