package hu.financial.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hu.financial.dto.report.ReportPeriod;
import hu.financial.dto.report.SummaryResponseDto;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.projection.TransactionTotals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Mock
    private TransactionRepository transactionRepository;

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
}
