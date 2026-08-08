package hu.financial.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hu.financial.dto.report.BudgetStatusItemDto;
import hu.financial.dto.report.BudgetStatusResponseDto;
import hu.financial.dto.report.CategoryBreakdownResponseDto;
import hu.financial.dto.report.ReportPeriod;
import hu.financial.dto.report.SummaryResponseDto;
import hu.financial.dto.report.TrendPeriod;
import hu.financial.dto.report.TrendResponseDto;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.BudgetRepository;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.projection.CategoryBudgetTotal;
import hu.financial.repository.projection.CategoryExpenseTotal;
import hu.financial.repository.projection.MonthlyTotals;
import hu.financial.repository.projection.TransactionTotals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long USER_ID = 1L;

    private static final YearMonth SERVER_MONTH = YearMonth.of(2026, 8);

    private static final ReportPeriod JULY = ReportPeriod.of("2026-07", SERVER_MONTH);

    private static final TrendPeriod SIX_MONTHS_TO_AUGUST = TrendPeriod.of("2026-08", 6, SERVER_MONTH);

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private ReportService reportService;

    private void givenTotals(TransactionTotals totals) {
        when(transactionRepository.summarize(anyLong(), any(), any(), any(), any(), any(), any())).thenReturn(totals);
    }

    private static TransactionTotals totals(String currentIncome, String currentExpense,
            String previousIncome, String previousExpense,
            String incomeUntilCurrentEnd, String expenseUntilCurrentEnd,
            String incomeUntilPreviousEnd, String expenseUntilPreviousEnd) {
        return new TransactionTotals(
                new BigDecimal(currentIncome),
                new BigDecimal(currentExpense),
                new BigDecimal(previousIncome),
                new BigDecimal(previousExpense),
                new BigDecimal(incomeUntilCurrentEnd),
                new BigDecimal(expenseUntilCurrentEnd),
                new BigDecimal(incomeUntilPreviousEnd),
                new BigDecimal(expenseUntilPreviousEnd));
    }

    private SummaryResponseDto summarize() {
        return reportService.summarize(USER_ID, JULY);
    }

    @Test
    void summarize_LabelsBothTheRequestedMonthAndItsPredecessor() {
        givenTotals(totals("500", "200", "400", "100", "1900", "300", "1400", "100"));

        SummaryResponseDto summary = summarize();

        assertEquals("2026-07", summary.month());
        assertEquals("2026-06", summary.previousMonth());
    }

    @Test
    void summarize_TypicalMonth_ComputesEveryMetricFromTheSingleQuery() {
        givenTotals(totals("500", "200", "400", "100", "1900", "300", "1400", "100"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("1600.00"), summary.balance().current());
        assertEquals(new BigDecimal("1300.00"), summary.balance().previous());
        assertEquals(new BigDecimal("23.1"), summary.balance().deltaPercent());

        assertEquals(new BigDecimal("500.00"), summary.income().current());
        assertEquals(new BigDecimal("400.00"), summary.income().previous());
        assertEquals(new BigDecimal("25.0"), summary.income().deltaPercent());

        assertEquals(new BigDecimal("200.00"), summary.expense().current());
        assertEquals(new BigDecimal("100.00"), summary.expense().previous());
        assertEquals(new BigDecimal("100.0"), summary.expense().deltaPercent());

        assertEquals(new BigDecimal("60.0"), summary.savingsRate().current());
        assertEquals(new BigDecimal("75.0"), summary.savingsRate().previous());
        assertEquals(new BigDecimal("-15.0"), summary.savingsRate().deltaPoints());
    }

    @Test
    void summarize_RunsExactlyOneQuery_ScopedToTheUserAndBothMonthBoundaries() {
        givenTotals(totals("500", "200", "400", "100", "1900", "300", "1400", "100"));

        summarize();

        verify(transactionRepository).summarize(
                eq(USER_ID),
                eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 7, 31)),
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE));
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    void summarize_BalanceComesFromTheCumulativeColumns_NotFromTheMonthlyOnes() {
        givenTotals(totals("500", "200", "400", "100", "1900", "300", "1400", "100"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("1600.00"), summary.balance().current());
        assertEquals(new BigDecimal("1300.00"), summary.balance().previous());
    }

    @Test
    void summarize_BalanceCurrentAndPreviousDiffer_BecauseEachHasItsOwnUpperDateBound() {
        givenTotals(totals("500", "200", "0", "0", "1900", "300", "1400", "100"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("1600.00"), summary.balance().current());
        assertEquals(new BigDecimal("1300.00"), summary.balance().previous());
    }

    @Test
    void summarize_ZeroPreviousBalance_YieldsNullDeltaPercentInsteadOfZeroOrInfinity() {
        givenTotals(totals("500", "200", "0", "0", "500", "200", "0", "0"));

        SummaryResponseDto summary = summarize();

        assertNull(summary.balance().deltaPercent());
        assertNull(summary.income().deltaPercent());
        assertNull(summary.expense().deltaPercent());
    }

    @Test
    void summarize_NewUsersFirstMonth_HasNullDeltasOnEveryMetricButRealCurrentValues() {
        givenTotals(totals("500", "200", "0", "0", "500", "200", "0", "0"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("300.00"), summary.balance().current());
        assertEquals(new BigDecimal("0.00"), summary.balance().previous());
        assertEquals(new BigDecimal("500.00"), summary.income().current());
        assertEquals(new BigDecimal("0.00"), summary.income().previous());
        assertEquals(new BigDecimal("60.0"), summary.savingsRate().current());
        assertNull(summary.savingsRate().previous());
        assertNull(summary.savingsRate().deltaPoints());
    }

    @Test
    void summarize_ZeroIncomeInTheRequestedMonth_YieldsNullSavingsRateCurrent() {
        givenTotals(totals("0", "200", "400", "100", "1400", "300", "1400", "100"));

        SummaryResponseDto summary = summarize();

        assertNull(summary.savingsRate().current());
        assertEquals(new BigDecimal("75.0"), summary.savingsRate().previous());
        assertNull(summary.savingsRate().deltaPoints());
    }

    @Test
    void summarize_ZeroIncomeInThePreviousMonth_YieldsNullSavingsRatePreviousAndNullDeltaPoints() {
        givenTotals(totals("500", "200", "0", "100", "1900", "400", "1400", "200"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("60.0"), summary.savingsRate().current());
        assertNull(summary.savingsRate().previous());
        assertNull(summary.savingsRate().deltaPoints());
    }

    @Test
    void summarize_UserWithoutAnyTransactions_IsAllZeroMoneyAndAllNullRatesAndDeltas() {
        givenTotals(new TransactionTotals(null, null, null, null, null, null, null, null));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("0.00"), summary.balance().current());
        assertEquals(new BigDecimal("0.00"), summary.balance().previous());
        assertEquals(new BigDecimal("0.00"), summary.income().current());
        assertEquals(new BigDecimal("0.00"), summary.income().previous());
        assertEquals(new BigDecimal("0.00"), summary.expense().current());
        assertEquals(new BigDecimal("0.00"), summary.expense().previous());
        assertNull(summary.balance().deltaPercent());
        assertNull(summary.income().deltaPercent());
        assertNull(summary.expense().deltaPercent());
        assertNull(summary.savingsRate().current());
        assertNull(summary.savingsRate().previous());
        assertNull(summary.savingsRate().deltaPoints());
    }

    @Test
    void summarize_GrowingSpending_ProducesAPositiveExpenseDelta_BecauseExpenseIsAPositiveMagnitude() {
        givenTotals(totals("500", "300", "500", "200", "1000", "500", "500", "200"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("300.00"), summary.expense().current());
        assertEquals(new BigDecimal("200.00"), summary.expense().previous());
        assertEquals(new BigDecimal("50.0"), summary.expense().deltaPercent());
    }

    @Test
    void summarize_ShrinkingSpending_ProducesANegativeExpenseDelta() {
        givenTotals(totals("500", "100", "500", "200", "1000", "300", "500", "200"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("-50.0"), summary.expense().deltaPercent());
    }

    @Test
    void summarize_NegativeBalanceAndNegativeSavingsRate_AreReportedAsIsWithoutClamping() {
        givenTotals(totals("100", "500", "100", "200", "200", "1000", "100", "500"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("-800.00"), summary.balance().current());
        assertEquals(new BigDecimal("-400.00"), summary.balance().previous());
        assertEquals(new BigDecimal("-400.0"), summary.savingsRate().current());
    }

    @Test
    void summarize_DeepeningDebt_YieldsANegativeDelta_BecauseTheDenominatorIsTheAbsolutePreviousValue() {
        givenTotals(totals("100", "500", "100", "200", "200", "1000", "100", "500"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("-400.00"), summary.balance().previous());
        assertEquals(new BigDecimal("-800.00"), summary.balance().current());
        assertEquals(new BigDecimal("-100.0"), summary.balance().deltaPercent());
    }

    @Test
    void summarize_ShrinkingDebt_YieldsAPositiveDelta_BecauseLessDebtIsAnImprovement() {
        givenTotals(totals("400", "0", "100", "200", "600", "1000", "200", "1000"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("-800.00"), summary.balance().previous());
        assertEquals(new BigDecimal("-400.00"), summary.balance().current());
        assertEquals(new BigDecimal("50.0"), summary.balance().deltaPercent());
    }

    @Test
    void summarize_DebtTurningIntoSurplus_YieldsAPositiveDelta_BecauseCrossingZeroUpwardIsAnImprovement() {
        givenTotals(totals("500", "0", "100", "200", "600", "500", "100", "500"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("-400.00"), summary.balance().previous());
        assertEquals(new BigDecimal("100.00"), summary.balance().current());
        assertEquals(new BigDecimal("125.0"), summary.balance().deltaPercent());
    }

    @Test
    void summarize_RoundsMoneyToTwoDecimalsHalfUp() {
        givenTotals(totals("10.005", "0", "1", "0", "10.005", "0", "1", "0"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("10.01"), summary.income().current());
        assertEquals(new BigDecimal("10.01"), summary.balance().current());
    }

    @Test
    void summarize_RoundsPercentToOneDecimalHalfUp() {
        givenTotals(totals("1000.50", "0", "1000", "0", "1000.50", "0", "1000", "0"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("0.1"), summary.income().deltaPercent());
    }

    @Test
    void summarize_RoundsSavingsRateToOneDecimalHalfUp() {
        givenTotals(totals("3", "1", "3", "1", "3", "1", "3", "1"));

        SummaryResponseDto summary = summarize();

        assertEquals(new BigDecimal("66.7"), summary.savingsRate().current());
        assertEquals(new BigDecimal("0.0"), summary.savingsRate().deltaPoints());
    }

    @Test
    void summarize_PassesTheGivenUserIdIntoTheQuery_SoTheFilterCannotBeLost() {
        givenTotals(totals("0", "0", "0", "0", "0", "0", "0", "0"));

        reportService.summarize(42L, JULY);

        verify(transactionRepository).summarize(eq(42L), any(), any(), any(), any(), any(), any());
    }

    private void givenCategoryRows(List<CategoryExpenseTotal> rows) {
        when(transactionRepository.summarizeExpensesByCategory(anyLong(), any(), any(), any())).thenReturn(rows);
    }

    private void givenMonthlyRows(List<MonthlyTotals> rows) {
        when(transactionRepository.summarizeMonthlyTotals(anyLong(), any(), any(), any(), any())).thenReturn(rows);
    }

    private static CategoryExpenseTotal categoryRow(long id, String name, String total) {
        return new CategoryExpenseTotal(id, name, new BigDecimal(total));
    }

    private static MonthlyTotals monthlyRow(int year, int month, String income, String expense) {
        return new MonthlyTotals(year, month, new BigDecimal(income), new BigDecimal(expense));
    }

    private CategoryBreakdownResponseDto breakdown() {
        return reportService.breakdownExpensesByCategory(USER_ID, JULY);
    }

    private TrendResponseDto trend() {
        return reportService.trend(USER_ID, SIX_MONTHS_TO_AUGUST);
    }

    @Test
    void breakdown_LabelsTheRequestedMonthAndSumsTheCategoryTotals() {
        givenCategoryRows(List.of(
                categoryRow(4L, "Lakhatás", "145000"),
                categoryRow(1L, "Élelmiszer", "92000")));

        CategoryBreakdownResponseDto breakdown = breakdown();

        assertEquals("2026-07", breakdown.month());
        assertEquals(new BigDecimal("237000.00"), breakdown.total());
        assertEquals(2, breakdown.categories().size());
    }

    @Test
    void breakdown_CarriesTheCategoryIdAndNameAlongsideTheMoney() {
        givenCategoryRows(List.of(categoryRow(4L, "Lakhatás", "145000")));

        CategoryBreakdownResponseDto breakdown = breakdown();

        assertEquals(4L, breakdown.categories().get(0).categoryId());
        assertEquals("Lakhatás", breakdown.categories().get(0).categoryName());
        assertEquals(new BigDecimal("145000.00"), breakdown.categories().get(0).total());
    }

    @Test
    void breakdown_ComputesEachShareAgainstTheMonthTotal() {
        givenCategoryRows(List.of(
                categoryRow(4L, "Lakhatás", "150000"),
                categoryRow(1L, "Élelmiszer", "90000"),
                categoryRow(2L, "Utazás", "60000")));

        CategoryBreakdownResponseDto breakdown = breakdown();

        assertEquals(new BigDecimal("300000.00"), breakdown.total());
        assertEquals(new BigDecimal("50.0"), breakdown.categories().get(0).percentage());
        assertEquals(new BigDecimal("30.0"), breakdown.categories().get(1).percentage());
        assertEquals(new BigDecimal("20.0"), breakdown.categories().get(2).percentage());
    }

    @Test
    void breakdown_RoundedSharesDoNotAddUpToOneHundred_AndThatIsTheExpectedBehaviour() {
        givenCategoryRows(List.of(
                categoryRow(1L, "egyik", "100"),
                categoryRow(2L, "másik", "100"),
                categoryRow(3L, "harmadik", "100")));

        CategoryBreakdownResponseDto breakdown = breakdown();

        BigDecimal sum = breakdown.categories().stream()
                .map(category -> category.percentage())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("33.3"), breakdown.categories().get(0).percentage());
        assertEquals(new BigDecimal("33.3"), breakdown.categories().get(1).percentage());
        assertEquals(new BigDecimal("33.3"), breakdown.categories().get(2).percentage());
        assertEquals(new BigDecimal("99.9"), sum);
        assertNotEquals(new BigDecimal("100.0"), sum);
    }

    @Test
    void breakdown_LeavesTheRemainderWhereItFalls_RatherThanPaddingTheLastRow() {
        givenCategoryRows(List.of(
                categoryRow(1L, "nagy", "2"),
                categoryRow(2L, "kicsi", "1")));

        CategoryBreakdownResponseDto breakdown = breakdown();

        assertEquals(new BigDecimal("66.7"), breakdown.categories().get(0).percentage());
        assertEquals(new BigDecimal("33.3"), breakdown.categories().get(1).percentage());
    }

    @Test
    void breakdown_PreservesTheOrderTheQueryReturned_WithoutReSortingInJava() {
        givenCategoryRows(List.of(
                categoryRow(9L, "harmadik", "500"),
                categoryRow(2L, "első", "500"),
                categoryRow(7L, "második", "100")));

        CategoryBreakdownResponseDto breakdown = breakdown();

        assertEquals(9L, breakdown.categories().get(0).categoryId());
        assertEquals(2L, breakdown.categories().get(1).categoryId());
        assertEquals(7L, breakdown.categories().get(2).categoryId());
    }

    @Test
    void breakdown_MonthWithoutExpenses_IsZeroTotalAndAnEmptyListRatherThanAFailure() {
        givenCategoryRows(List.of());

        CategoryBreakdownResponseDto breakdown = breakdown();

        assertEquals("2026-07", breakdown.month());
        assertEquals(new BigDecimal("0.00"), breakdown.total());
        assertTrue(breakdown.categories().isEmpty());
    }

    @Test
    void breakdown_RoundsMoneyToTwoDecimalsAndSharesToOneDecimalHalfUp() {
        givenCategoryRows(List.of(
                categoryRow(1L, "egyik", "10.005"),
                categoryRow(2L, "másik", "4.995")));

        CategoryBreakdownResponseDto breakdown = breakdown();

        assertEquals(new BigDecimal("10.01"), breakdown.categories().get(0).total());
        assertEquals(new BigDecimal("5.00"), breakdown.categories().get(1).total());
        assertEquals(new BigDecimal("15.00"), breakdown.total());
        assertEquals(new BigDecimal("66.7"), breakdown.categories().get(0).percentage());
        assertEquals(new BigDecimal("33.3"), breakdown.categories().get(1).percentage());
    }

    @Test
    void breakdown_RunsExactlyOneQuery_ScopedToTheUserTheMonthAndExpensesOnly() {
        givenCategoryRows(List.of());

        breakdown();

        verify(transactionRepository).summarizeExpensesByCategory(
                eq(USER_ID),
                eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 7, 31)),
                eq(TransactionType.EXPENSE));
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    void breakdown_PassesTheGivenUserIdIntoTheQuery_SoTheFilterCannotBeLost() {
        givenCategoryRows(List.of());

        reportService.breakdownExpensesByCategory(42L, JULY);

        verify(transactionRepository).summarizeExpensesByCategory(eq(42L), any(), any(), any());
    }

    @Test
    void trend_LabelsTheRequestedMonthAndTheSeriesLength() {
        givenMonthlyRows(List.of());

        TrendResponseDto trend = trend();

        assertEquals("2026-08", trend.month());
        assertEquals(6, trend.months());
    }

    @Test
    void trend_AMonthWithoutTransactionsInTheMiddleIsFilledWithZeros_NotDroppedFromTheSeries() {
        givenMonthlyRows(List.of(
                monthlyRow(2026, 3, "430000", "298000"),
                monthlyRow(2026, 4, "440000", "300000"),
                monthlyRow(2026, 6, "450000", "310000"),
                monthlyRow(2026, 7, "460000", "320000"),
                monthlyRow(2026, 8, "470000", "330000")));

        TrendResponseDto trend = trend();

        assertEquals(6, trend.points().size());
        assertEquals(List.of("2026-03", "2026-04", "2026-05", "2026-06", "2026-07", "2026-08"),
                trend.points().stream().map(point -> point.month()).toList());
        assertEquals("2026-05", trend.points().get(2).month());
        assertEquals(new BigDecimal("0.00"), trend.points().get(2).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(2).expense());
        assertEquals(new BigDecimal("450000.00"), trend.points().get(3).income());
        assertEquals(new BigDecimal("310000.00"), trend.points().get(3).expense());
    }

    @Test
    void trend_SeveralConsecutiveEmptyMonthsInTheMiddleAreAllFilled() {
        givenMonthlyRows(List.of(
                monthlyRow(2026, 3, "100", "10"),
                monthlyRow(2026, 8, "200", "20")));

        TrendResponseDto trend = trend();

        assertEquals(6, trend.points().size());
        assertEquals(new BigDecimal("100.00"), trend.points().get(0).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(1).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(2).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(3).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(4).income());
        assertEquals(new BigDecimal("200.00"), trend.points().get(5).income());
    }

    @Test
    void trend_PointsAreChronologicallyAscendingEvenWhenTheQueryRowsAreNot() {
        givenMonthlyRows(List.of(
                monthlyRow(2026, 8, "800", "80"),
                monthlyRow(2026, 3, "300", "30"),
                monthlyRow(2026, 6, "600", "60")));

        TrendResponseDto trend = trend();

        assertEquals(List.of("2026-03", "2026-04", "2026-05", "2026-06", "2026-07", "2026-08"),
                trend.points().stream().map(point -> point.month()).toList());
        assertEquals(new BigDecimal("300.00"), trend.points().get(0).income());
        assertEquals(new BigDecimal("600.00"), trend.points().get(3).income());
        assertEquals(new BigDecimal("800.00"), trend.points().get(5).income());
    }

    @Test
    void trend_UserWithoutAnyTransactions_GetsAFullLengthAllZeroSeriesRatherThanAnEmptyList() {
        givenMonthlyRows(List.of());

        TrendResponseDto trend = trend();

        assertEquals(6, trend.points().size());
        assertTrue(trend.points().stream()
                .allMatch(point -> point.income().compareTo(BigDecimal.ZERO) == 0
                        && point.expense().compareTo(BigDecimal.ZERO) == 0));
        assertEquals(new BigDecimal("0.00"), trend.points().get(0).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(5).expense());
    }

    @Test
    void trend_SeriesLengthAlwaysMatchesTheRequestedMonths() {
        givenMonthlyRows(List.of(monthlyRow(2026, 8, "1", "1")));

        assertEquals(1, reportService.trend(USER_ID, TrendPeriod.of("2026-08", 1, SERVER_MONTH)).points().size());
        assertEquals(12, reportService.trend(USER_ID, TrendPeriod.of("2026-08", 12, SERVER_MONTH)).points().size());
        assertEquals(24, reportService.trend(USER_ID, TrendPeriod.of("2026-08", 24, SERVER_MONTH)).points().size());
    }

    @Test
    void trend_CrossesTheYearBoundaryWithoutLosingOrDuplicatingAMonth() {
        givenMonthlyRows(List.of(
                monthlyRow(2025, 12, "1200", "120"),
                monthlyRow(2026, 1, "100", "10")));

        TrendResponseDto trend = reportService.trend(USER_ID, TrendPeriod.of("2026-02", 4, SERVER_MONTH));

        assertEquals(List.of("2025-11", "2025-12", "2026-01", "2026-02"),
                trend.points().stream().map(point -> point.month()).toList());
        assertEquals(new BigDecimal("0.00"), trend.points().get(0).income());
        assertEquals(new BigDecimal("1200.00"), trend.points().get(1).income());
        assertEquals(new BigDecimal("100.00"), trend.points().get(2).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(3).income());
    }

    @Test
    void trend_RoundsMoneyToTwoDecimalsHalfUp() {
        givenMonthlyRows(List.of(monthlyRow(2026, 8, "10.005", "0.004")));

        TrendResponseDto trend = trend();

        assertEquals(new BigDecimal("10.01"), trend.points().get(5).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(5).expense());
    }

    @Test
    void trend_RunsExactlyOneQuery_ScopedToTheUserAndTheWholeSeriesWindow() {
        givenMonthlyRows(List.of());

        trend();

        verify(transactionRepository).summarizeMonthlyTotals(
                eq(USER_ID),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 8, 31)),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE));
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    void trend_PassesTheGivenUserIdIntoTheQuery_SoTheFilterCannotBeLost() {
        givenMonthlyRows(List.of());

        reportService.trend(42L, SIX_MONTHS_TO_AUGUST);

        verify(transactionRepository).summarizeMonthlyTotals(eq(42L), any(), any(), any(), any());
    }

    private void givenBudgetRows(List<CategoryBudgetTotal> rows) {
        when(budgetRepository.summarizeBudgetsByCategory(anyLong(), any(), any())).thenReturn(rows);
    }

    private static CategoryBudgetTotal budgetRow(long id, String name, String total) {
        return new CategoryBudgetTotal(id, name, new BigDecimal(total));
    }

    private BudgetStatusResponseDto budgetStatus() {
        return reportService.budgetStatus(USER_ID, JULY);
    }

    private static List<Long> statusCategoryIds(BudgetStatusResponseDto status) {
        return status.categories().stream().map(BudgetStatusItemDto::categoryId).toList();
    }

    @Test
    void budgetStatus_LabelsTheMonthAndCarriesEveryBudgetedCategory() {
        givenBudgetRows(List.of(
                budgetRow(4L, "Lakhatás", "150000"),
                budgetRow(1L, "Élelmiszer", "100000")));
        givenCategoryRows(List.of(
                categoryRow(4L, "Lakhatás", "162000"),
                categoryRow(1L, "Élelmiszer", "36000")));

        BudgetStatusResponseDto status = budgetStatus();

        assertEquals("2026-07", status.month());
        assertEquals(2, status.categories().size());
        assertEquals(new BigDecimal("250000.00"), status.totalBudgeted());
        assertEquals(new BigDecimal("198000.00"), status.totalSpent());
        assertEquals(new BigDecimal("0.00"), status.unbudgetedSpending());
    }

    @Test
    void budgetStatus_CarriesTheCategoryIdAndNameAlongsideTheMoney() {
        givenBudgetRows(List.of(budgetRow(4L, "Lakhatás", "150000")));
        givenCategoryRows(List.of(categoryRow(4L, "Lakhatás", "162000")));

        BudgetStatusItemDto item = budgetStatus().categories().get(0);

        assertEquals(4L, item.categoryId());
        assertEquals("Lakhatás", item.categoryName());
        assertEquals(new BigDecimal("150000.00"), item.budgeted());
        assertEquals(new BigDecimal("162000.00"), item.spent());
    }

    @Test
    void budgetStatus_Overspending_IsReportedAboveOneHundredWithoutClamping() {
        givenBudgetRows(List.of(budgetRow(4L, "Lakhatás", "150000")));
        givenCategoryRows(List.of(categoryRow(4L, "Lakhatás", "162000")));

        BudgetStatusItemDto item = budgetStatus().categories().get(0);

        assertEquals(new BigDecimal("108.0"), item.percentageUsed());
        assertNotEquals(new BigDecimal("100.0"), item.percentageUsed());
    }

    @Test
    void budgetStatus_RemainingIsSigned_SoOverspendingIsNegativeRatherThanAMagnitude() {
        givenBudgetRows(List.of(
                budgetRow(4L, "Lakhatás", "150000"),
                budgetRow(1L, "Élelmiszer", "100000")));
        givenCategoryRows(List.of(
                categoryRow(4L, "Lakhatás", "162000"),
                categoryRow(1L, "Élelmiszer", "36000")));

        BudgetStatusResponseDto status = budgetStatus();

        assertEquals(new BigDecimal("-12000.00"), status.categories().get(0).remaining());
        assertNotEquals(new BigDecimal("12000.00"), status.categories().get(0).remaining());
        assertEquals(new BigDecimal("64000.00"), status.categories().get(1).remaining());
    }

    @Test
    void budgetStatus_BudgetWithoutSpending_IsARowWithZeroSpentAndZeroPercentage() {
        givenBudgetRows(List.of(budgetRow(7L, "Megtakarítás", "30000")));
        givenCategoryRows(List.of());

        BudgetStatusItemDto item = budgetStatus().categories().get(0);

        assertEquals(new BigDecimal("30000.00"), item.budgeted());
        assertEquals(new BigDecimal("0.00"), item.spent());
        assertEquals(new BigDecimal("30000.00"), item.remaining());
        assertEquals(new BigDecimal("0.0"), item.percentageUsed());
    }

    @Test
    void budgetStatus_ZeroBudget_YieldsNullPercentageUsedInsteadOfInfinity() {
        givenBudgetRows(List.of(budgetRow(3L, "Utazás", "0")));
        givenCategoryRows(List.of(categoryRow(3L, "Utazás", "5000")));

        BudgetStatusItemDto item = budgetStatus().categories().get(0);

        assertEquals(new BigDecimal("0.00"), item.budgeted());
        assertEquals(new BigDecimal("5000.00"), item.spent());
        assertEquals(new BigDecimal("-5000.00"), item.remaining());
        assertNull(item.percentageUsed());
    }

    @Test
    void budgetStatus_SpendingWithoutABudget_IsNotARowButIsCountedAsUnbudgetedSpending() {
        givenBudgetRows(List.of(budgetRow(4L, "Lakhatás", "150000")));
        givenCategoryRows(List.of(
                categoryRow(4L, "Lakhatás", "162000"),
                categoryRow(9L, "Szórakozás", "42000")));

        BudgetStatusResponseDto status = budgetStatus();

        assertEquals(List.of(4L), statusCategoryIds(status));
        assertEquals(new BigDecimal("42000.00"), status.unbudgetedSpending());
        assertEquals(new BigDecimal("162000.00"), status.totalSpent());
    }

    @Test
    void budgetStatus_TotalSpentCountsOnlyTheBudgetedCategories_AndIsNotTheWholeMonthSpend() {
        givenBudgetRows(List.of(budgetRow(4L, "Lakhatás", "150000")));
        givenCategoryRows(List.of(
                categoryRow(4L, "Lakhatás", "162000"),
                categoryRow(9L, "Szórakozás", "42000")));

        BudgetStatusResponseDto status = budgetStatus();

        assertEquals(new BigDecimal("162000.00"), status.totalSpent());
        assertNotEquals(new BigDecimal("204000.00"), status.totalSpent());
    }

    @Test
    void budgetStatus_MonthWithoutBudgets_IsAnEmptyListWhoseUnbudgetedSpendingIsTheWholeMonthSpend() {
        givenBudgetRows(List.of());
        givenCategoryRows(List.of(
                categoryRow(4L, "Lakhatás", "888888"),
                categoryRow(9L, "Szórakozás", "1112")));

        BudgetStatusResponseDto status = budgetStatus();

        assertEquals("2026-07", status.month());
        assertTrue(status.categories().isEmpty());
        assertEquals(new BigDecimal("0.00"), status.totalBudgeted());
        assertEquals(new BigDecimal("0.00"), status.totalSpent());
        assertEquals(new BigDecimal("890000.00"), status.unbudgetedSpending());
        assertNotEquals(new BigDecimal("0.00"), status.unbudgetedSpending());
    }

    @Test
    void budgetStatus_UserWithNeitherBudgetsNorSpending_IsAllZerosAndAnEmptyList() {
        givenBudgetRows(List.of());
        givenCategoryRows(List.of());

        BudgetStatusResponseDto status = budgetStatus();

        assertTrue(status.categories().isEmpty());
        assertEquals(new BigDecimal("0.00"), status.totalBudgeted());
        assertEquals(new BigDecimal("0.00"), status.totalSpent());
        assertEquals(new BigDecimal("0.00"), status.unbudgetedSpending());
    }

    @Test
    void budgetStatus_OrdersByPercentageUsedDescendingWithTheUndefinedOnesLast() {
        givenBudgetRows(List.of(
                budgetRow(1L, "nulla keret", "0"),
                budgetRow(2L, "alig", "100000"),
                budgetRow(3L, "túl", "10000"),
                budgetRow(4L, "közepes", "10000")));
        givenCategoryRows(List.of(
                categoryRow(1L, "nulla keret", "500"),
                categoryRow(2L, "alig", "1000"),
                categoryRow(3L, "túl", "20000"),
                categoryRow(4L, "közepes", "5000")));

        BudgetStatusResponseDto status = budgetStatus();

        assertEquals(List.of(3L, 4L, 2L, 1L), statusCategoryIds(status));
        assertEquals(new BigDecimal("200.0"), status.categories().get(0).percentageUsed());
        assertEquals(new BigDecimal("50.0"), status.categories().get(1).percentageUsed());
        assertEquals(new BigDecimal("1.0"), status.categories().get(2).percentageUsed());
        assertNull(status.categories().get(3).percentageUsed());
    }

    @Test
    void budgetStatus_BreaksEqualPercentagesByAscendingCategoryId() {
        givenBudgetRows(List.of(
                budgetRow(9L, "kilenc", "10000"),
                budgetRow(2L, "kettő", "20000"),
                budgetRow(7L, "hét", "40000")));
        givenCategoryRows(List.of(
                categoryRow(9L, "kilenc", "5000"),
                categoryRow(2L, "kettő", "10000"),
                categoryRow(7L, "hét", "20000")));

        BudgetStatusResponseDto status = budgetStatus();

        assertEquals(List.of(2L, 7L, 9L), statusCategoryIds(status));
        assertTrue(status.categories().stream()
                .allMatch(item -> new BigDecimal("50.0").equals(item.percentageUsed())));
    }

    @Test
    void budgetStatus_TwoUndefinedPercentages_AreStillOrderedByAscendingCategoryId() {
        givenBudgetRows(List.of(
                budgetRow(9L, "kilenc", "0"),
                budgetRow(2L, "kettő", "0")));
        givenCategoryRows(List.of());

        BudgetStatusResponseDto status = budgetStatus();

        assertEquals(List.of(2L, 9L), statusCategoryIds(status));
    }

    @Test
    void budgetStatus_RoundsMoneyToTwoDecimalsAndPercentageToOneDecimalHalfUp() {
        givenBudgetRows(List.of(budgetRow(1L, "kerekítés", "3")));
        givenCategoryRows(List.of(categoryRow(1L, "kerekítés", "1.005")));

        BudgetStatusItemDto item = budgetStatus().categories().get(0);

        assertEquals(new BigDecimal("3.00"), item.budgeted());
        assertEquals(new BigDecimal("1.01"), item.spent());
        assertEquals(new BigDecimal("2.00"), item.remaining());
        assertEquals(new BigDecimal("33.5"), item.percentageUsed());
    }

    @Test
    void budgetStatus_RunsExactlyTwoQueries_EachScopedToTheUserAndTheRequestedMonth() {
        givenBudgetRows(List.of());
        givenCategoryRows(List.of());

        budgetStatus();

        verify(budgetRepository).summarizeBudgetsByCategory(
                eq(USER_ID),
                eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 7, 31)));
        verify(transactionRepository).summarizeExpensesByCategory(
                eq(USER_ID),
                eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 7, 31)),
                eq(TransactionType.EXPENSE));
        verifyNoMoreInteractions(budgetRepository, transactionRepository);
    }

    @Test
    void budgetStatus_AsksTheTransactionSideForExpensesOnly_SoIncomeCannotOffsetTheSpending() {
        givenBudgetRows(List.of());
        givenCategoryRows(List.of());

        budgetStatus();

        verify(transactionRepository).summarizeExpensesByCategory(anyLong(), any(), any(),
                eq(TransactionType.EXPENSE));
    }

    @Test
    void budgetStatus_PassesTheGivenUserIdIntoBothQueries_SoNeitherFilterCanBeLost() {
        givenBudgetRows(List.of());
        givenCategoryRows(List.of());

        reportService.budgetStatus(42L, JULY);

        verify(budgetRepository).summarizeBudgetsByCategory(eq(42L), any(), any());
        verify(transactionRepository).summarizeExpensesByCategory(eq(42L), any(), any(), any());
    }
}
