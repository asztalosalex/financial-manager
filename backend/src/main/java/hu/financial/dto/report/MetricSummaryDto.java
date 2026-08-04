package hu.financial.dto.report;

import java.math.BigDecimal;

public record MetricSummaryDto(BigDecimal current, BigDecimal previous, BigDecimal deltaPercent) {
}
