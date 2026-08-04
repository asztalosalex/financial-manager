package hu.financial.dto.report;

public record SummaryResponseDto(
        String month,
        String previousMonth,
        MetricSummaryDto balance,
        MetricSummaryDto income,
        MetricSummaryDto expense,
        SavingsRateSummaryDto savingsRate) {
}
