package hu.financial.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.financial.dto.report.BudgetStatusItemDto;
import hu.financial.dto.report.BudgetStatusResponseDto;
import hu.financial.dto.report.CategoryBreakdownItemDto;
import hu.financial.dto.report.CategoryBreakdownResponseDto;
import hu.financial.dto.report.MetricSummaryDto;
import hu.financial.dto.report.ReportPeriod;
import hu.financial.dto.report.SavingsRateSummaryDto;
import hu.financial.dto.report.SummaryResponseDto;
import hu.financial.dto.report.TrendPeriod;
import hu.financial.dto.report.TrendPointDto;
import hu.financial.dto.report.TrendResponseDto;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.BudgetRepository;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.projection.CategoryBudgetTotal;
import hu.financial.repository.projection.CategoryExpenseTotal;
import hu.financial.repository.projection.MonthlyTotals;
import hu.financial.repository.projection.TransactionTotals;

@Service
public class ReportService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final int MONEY_SCALE = 2;

    private static final int PERCENT_SCALE = 1;

    private static final Comparator<BudgetStatusItemDto> BY_RISK_THEN_CATEGORY = Comparator
            .comparing(BudgetStatusItemDto::percentageUsed,
                    Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder()))
            .thenComparing(BudgetStatusItemDto::categoryId, Comparator.<Long>naturalOrder());

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

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

    @Transactional(readOnly = true)
    public CategoryBreakdownResponseDto breakdownExpensesByCategory(Long userId, ReportPeriod period) {
        List<CategoryExpenseTotal> rows = transactionRepository.summarizeExpensesByCategory(
                userId,
                period.start(),
                period.end(),
                TransactionType.EXPENSE);

        BigDecimal total = rows.stream()
                .map(CategoryExpenseTotal::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryBreakdownItemDto> categories = rows.stream()
                .map(row -> new CategoryBreakdownItemDto(
                        row.categoryId(),
                        row.categoryName(),
                        money(row.total()),
                        share(row.total(), total)))
                .toList();

        return new CategoryBreakdownResponseDto(period.month().toString(), money(total), categories);
    }

    @Transactional(readOnly = true)
    public TrendResponseDto trend(Long userId, TrendPeriod period) {
        Map<YearMonth, MonthlyTotals> byMonth = transactionRepository.summarizeMonthlyTotals(
                userId,
                period.start(),
                period.end(),
                TransactionType.INCOME,
                TransactionType.EXPENSE)
                .stream()
                .collect(Collectors.toMap(MonthlyTotals::yearMonth, Function.identity()));

        List<TrendPointDto> points = period.monthsInOrder().stream()
                .map(month -> point(month, byMonth.get(month)))
                .toList();

        return new TrendResponseDto(period.month().toString(), period.months(), points);
    }

    @Transactional(readOnly = true)
    public BudgetStatusResponseDto budgetStatus(Long userId, ReportPeriod period) {
        List<CategoryBudgetTotal> budgets = budgetRepository.summarizeBudgetsByCategory(
                userId,
                period.start(),
                period.end());

        List<CategoryExpenseTotal> expenses = transactionRepository.summarizeExpensesByCategory(
                userId,
                period.start(),
                period.end(),
                TransactionType.EXPENSE);

        Map<Long, BigDecimal> spentByCategory = expenses.stream()
                .collect(Collectors.toMap(CategoryExpenseTotal::categoryId, CategoryExpenseTotal::total));

        Set<Long> budgetedCategoryIds = budgets.stream()
                .map(CategoryBudgetTotal::categoryId)
                .collect(Collectors.toSet());

        List<BudgetStatusItemDto> categories = budgets.stream()
                .map(budget -> statusItem(budget, spentByCategory.getOrDefault(budget.categoryId(), BigDecimal.ZERO)))
                .sorted(BY_RISK_THEN_CATEGORY)
                .toList();

        BigDecimal totalBudgeted = sum(categories.stream().map(BudgetStatusItemDto::budgeted));
        BigDecimal totalSpent = sum(categories.stream().map(BudgetStatusItemDto::spent));
        BigDecimal unbudgetedSpending = sum(expenses.stream()
                .filter(expense -> !budgetedCategoryIds.contains(expense.categoryId()))
                .map(CategoryExpenseTotal::total));

        return new BudgetStatusResponseDto(
                period.month().toString(),
                money(totalBudgeted),
                money(totalSpent),
                money(unbudgetedSpending),
                categories);
    }

    private static BudgetStatusItemDto statusItem(CategoryBudgetTotal budget, BigDecimal spent) {
        return new BudgetStatusItemDto(
                budget.categoryId(),
                budget.categoryName(),
                money(budget.total()),
                money(spent),
                money(budget.total().subtract(spent)),
                share(spent, budget.total()));
    }

    private static BigDecimal sum(Stream<BigDecimal> values) {
        return values.reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static TrendPointDto point(YearMonth month, MonthlyTotals totals) {
        BigDecimal income = totals == null ? BigDecimal.ZERO : totals.income();
        BigDecimal expense = totals == null ? BigDecimal.ZERO : totals.expense();
        return new TrendPointDto(month.toString(), money(income), money(expense));
    }

    private static BigDecimal share(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(total, PERCENT_SCALE, RoundingMode.HALF_UP);
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
