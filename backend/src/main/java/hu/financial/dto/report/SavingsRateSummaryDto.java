package hu.financial.dto.report;

import java.math.BigDecimal;

public record SavingsRateSummaryDto(BigDecimal current, BigDecimal previous, BigDecimal deltaPoints) {
}
