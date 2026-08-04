package hu.financial.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.financial.dto.report.MetricSummaryDto;
import hu.financial.dto.report.ReportPeriod;
import hu.financial.dto.report.SavingsRateSummaryDto;
import hu.financial.dto.report.SummaryResponseDto;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.projection.TransactionTotals;

@Service
public class ReportService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final int MONEY_SCALE = 2;

    private static final int PERCENT_SCALE = 1;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public SummaryResponseDto summarize(Long userId, ReportPeriod period) {
        TransactionTotals totals = transactionRepository.summarize(
                userId,
                period.start(),
                period.end(),
                period.previousStart(),
                period.previousEnd(),
                TransactionType.INCOME,
                TransactionType.EXPENSE);

        return new SummaryResponseDto(
                period.month().toString(),
                period.previousMonth().toString(),
                metric(totals.currentBalance(), totals.previousBalance()),
                metric(totals.currentIncome(), totals.previousIncome()),
                metric(totals.currentExpense(), totals.previousExpense()),
                savingsRate(totals));
    }

    private static MetricSummaryDto metric(BigDecimal current, BigDecimal previous) {
        return new MetricSummaryDto(money(current), money(previous), relativeChange(current, previous));
    }

    private static SavingsRateSummaryDto savingsRate(TransactionTotals totals) {
        BigDecimal current = rate(totals.currentIncome(), totals.currentExpense());
        BigDecimal previous = rate(totals.previousIncome(), totals.previousExpense());
        return new SavingsRateSummaryDto(current, previous, difference(current, previous));
    }

    private static BigDecimal rate(BigDecimal income, BigDecimal expense) {
        if (income.signum() == 0) {
            return null;
        }
        return income.subtract(expense).multiply(HUNDRED).divide(income, PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal relativeChange(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(HUNDRED).divide(previous.abs(), PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal difference(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) {
            return null;
        }
        return current.subtract(previous).setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
