package hu.financial.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import hu.financial.config.FilterRegistrationConfig;
import hu.financial.config.SecurityConfig;
import hu.financial.filter.JwtAuthenticationFilter;
import hu.financial.model.User;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.BudgetRepository;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.projection.CategoryBudgetTotal;
import hu.financial.repository.projection.CategoryExpenseTotal;
import hu.financial.repository.projection.MonthlyTotals;
import hu.financial.repository.projection.TransactionTotals;
import hu.financial.security.CookieProperties;
import hu.financial.security.CsrfCookieFilter;
import hu.financial.security.RestAccessDeniedHandler;
import hu.financial.security.SecurityCookieFactory;
import hu.financial.service.JwtService;
import hu.financial.service.ReportService;
import hu.financial.service.UserService;
import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CookieProperties.class, RestAccessDeniedHandler.class,
        SecurityCookieFactory.class, CsrfCookieFilter.class, JwtService.class, ReportService.class,
        FilterRegistrationConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.jwt.expiration-time=3600",
        "security.cookie.secure=false",
        "security.cookie.same-site=Lax"
})
class ReportControllerMvcTest {

    private static final Long CURRENT_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private BudgetRepository budgetRepository;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User("testuser", "encoded-password", "test@example.com");
        currentUser.setId(CURRENT_USER_ID);

        when(userService.loadUserByUsername("testuser")).thenReturn(currentUser);
        when(userService.getCurrentUser()).thenReturn(currentUser);
    }

    private Cookie authCookie() {
        return new Cookie("authToken", jwtService.generateToken(currentUser));
    }

    private void givenTotals(TransactionTotals totals) {
        when(transactionRepository.summarize(anyLong(), any(), any(), any(), any(), any(), any())).thenReturn(totals);
    }

    private static TransactionTotals populatedTotals() {
        return new TransactionTotals(
                new BigDecimal("500.00"),
                new BigDecimal("200.00"),
                new BigDecimal("400.00"),
                new BigDecimal("100.00"),
                new BigDecimal("1900.00"),
                new BigDecimal("300.00"),
                new BigDecimal("1400.00"),
                new BigDecimal("100.00"));
    }

    private static TransactionTotals emptyTotals() {
        return new TransactionTotals(null, null, null, null, null, null, null, null);
    }

    @Test
    void getSummary_ReturnsBothPeriodsAndEveryMetric() throws Exception {
        givenTotals(populatedTotals());

        mockMvc.perform(get("/api/reports/summary").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.month").value("2026-07"))
                .andExpect(jsonPath("$.previousMonth").value("2026-06"))
                .andExpect(jsonPath("$.balance.current").value(1600.00))
                .andExpect(jsonPath("$.balance.previous").value(1300.00))
                .andExpect(jsonPath("$.balance.deltaPercent").value(23.1))
                .andExpect(jsonPath("$.income.current").value(500.00))
                .andExpect(jsonPath("$.income.previous").value(400.00))
                .andExpect(jsonPath("$.income.deltaPercent").value(25.0))
                .andExpect(jsonPath("$.expense.current").value(200.00))
                .andExpect(jsonPath("$.expense.previous").value(100.00))
                .andExpect(jsonPath("$.expense.deltaPercent").value(100.0))
                .andExpect(jsonPath("$.savingsRate.current").value(60.0))
                .andExpect(jsonPath("$.savingsRate.previous").value(75.0))
                .andExpect(jsonPath("$.savingsRate.deltaPoints").value(-15.0));
    }

    @Test
    void getSummary_MoneyAndPercentGoOnTheWireAsJsonNumbers_NotStrings() throws Exception {
        givenTotals(populatedTotals());

        mockMvc.perform(get("/api/reports/summary").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance.current").isNumber())
                .andExpect(jsonPath("$.income.deltaPercent").isNumber())
                .andExpect(jsonPath("$.savingsRate.deltaPoints").isNumber())
                .andExpect(content().string(containsString("\"current\":1600.00")));
    }

    @Test
    void getSummary_SavingsRateCarriesDeltaPoints_AndTheOtherMetricsCarryDeltaPercent() throws Exception {
        givenTotals(populatedTotals());

        mockMvc.perform(get("/api/reports/summary").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savingsRate.deltaPoints").exists())
                .andExpect(jsonPath("$.savingsRate.deltaPercent").doesNotExist())
                .andExpect(jsonPath("$.balance.deltaPoints").doesNotExist())
                .andExpect(jsonPath("$.income.deltaPoints").doesNotExist())
                .andExpect(jsonPath("$.expense.deltaPoints").doesNotExist())
                .andExpect(jsonPath("$.balance.delta").doesNotExist());
    }

    @Test
    void getSummary_ForAUserWithoutTransactions_Is200WithZeroMoneyAndNullDeltas() throws Exception {
        givenTotals(emptyTotals());

        mockMvc.perform(get("/api/reports/summary").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance.current").value(0.00))
                .andExpect(jsonPath("$.income.current").value(0.00))
                .andExpect(jsonPath("$.expense.current").value(0.00))
                .andExpect(jsonPath("$.balance.deltaPercent").value(nullValue()))
                .andExpect(jsonPath("$.income.deltaPercent").value(nullValue()))
                .andExpect(jsonPath("$.expense.deltaPercent").value(nullValue()))
                .andExpect(jsonPath("$.savingsRate.current").value(nullValue()))
                .andExpect(jsonPath("$.savingsRate.previous").value(nullValue()))
                .andExpect(jsonPath("$.savingsRate.deltaPoints").value(nullValue()));
    }

    @Test
    void getSummary_NullMetrics_AreEmittedAsJsonNullNotOmittedFromTheBody() throws Exception {
        givenTotals(emptyTotals());

        mockMvc.perform(get("/api/reports/summary").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"deltaPercent\":null")))
                .andExpect(content().string(containsString("\"deltaPoints\":null")))
                .andExpect(jsonPath("$.balance.deltaPercent").hasJsonPath())
                .andExpect(jsonPath("$.savingsRate.deltaPoints").hasJsonPath());
    }

    @Test
    void getSummary_WithoutMonthParameter_UsesTheServerCalendarMonth() throws Exception {
        givenTotals(emptyTotals());
        YearMonth serverMonth = YearMonth.now();

        mockMvc.perform(get("/api/reports/summary").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(serverMonth.toString()))
                .andExpect(jsonPath("$.previousMonth").value(serverMonth.minusMonths(1).toString()));
    }

    @Test
    void getSummary_MalformedMonth_Returns400WithUnprefixedMonthFieldErrorAndNoQuery() throws Exception {
        mockMvc.perform(get("/api/reports/summary").param("month", "2026-07-01").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.month").exists())
                .andExpect(jsonPath("$.fieldErrors['getSummary.month']").doesNotExist());

        verify(transactionRepository, never()).summarize(anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getSummary_NonsenseMonth_Returns400() throws Exception {
        mockMvc.perform(get("/api/reports/summary").param("month", "last-month").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.month").exists());

        verify(transactionRepository, never()).summarize(anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getSummary_AFutureMonth_IsNotAnError() throws Exception {
        givenTotals(emptyTotals());

        mockMvc.perform(get("/api/reports/summary").param("month", "2099-12").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2099-12"))
                .andExpect(jsonPath("$.previousMonth").value("2099-11"))
                .andExpect(jsonPath("$.income.current").value(0.00));
    }

    @Test
    void getSummary_QueriesForTheAuthenticatedUserWithTheRequestedMonthBoundaries() throws Exception {
        givenTotals(emptyTotals());

        mockMvc.perform(get("/api/reports/summary").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk());

        verify(transactionRepository).summarize(
                eq(CURRENT_USER_ID),
                eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 7, 31)),
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 30)),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE));
    }

    @Test
    void getSummary_IgnoresAUserIdQueryParameter_AndStillQueriesTheAuthenticatedUser() throws Exception {
        givenTotals(emptyTotals());

        mockMvc.perform(get("/api/reports/summary")
                .param("month", "2026-07")
                .param("userId", "2")
                .cookie(authCookie()))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> userId = ArgumentCaptor.forClass(Long.class);
        verify(transactionRepository).summarize(userId.capture(), any(), any(), any(), any(), any(), any());
        assertEquals(CURRENT_USER_ID, userId.getValue());
    }

    @Test
    void getSummary_WithoutAuthCookie_Returns401AndNeverQueries() throws Exception {
        mockMvc.perform(get("/api/reports/summary").param("month", "2026-07"))
                .andExpect(status().isUnauthorized());

        verify(transactionRepository, never()).summarize(anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getSummary_RunsExactlyOneQueryPerRequest() throws Exception {
        givenTotals(populatedTotals());

        mockMvc.perform(get("/api/reports/summary").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk());

        verify(transactionRepository).summarize(anyLong(), any(), any(), any(), any(), any(), any());
    }

    private void givenCategoryRows(List<CategoryExpenseTotal> rows) {
        when(transactionRepository.summarizeExpensesByCategory(anyLong(), any(), any(), any())).thenReturn(rows);
    }

    private void givenMonthlyRows(List<MonthlyTotals> rows) {
        when(transactionRepository.summarizeMonthlyTotals(anyLong(), any(), any(), any(), any())).thenReturn(rows);
    }

    private static List<CategoryExpenseTotal> populatedCategoryRows() {
        return List.of(
                new CategoryExpenseTotal(4L, "Lakhatás", new BigDecimal("150000.00")),
                new CategoryExpenseTotal(1L, "Élelmiszer", new BigDecimal("90000.00")),
                new CategoryExpenseTotal(2L, "Utazás", new BigDecimal("60000.00")));
    }

    private static List<MonthlyTotals> monthlyRowsWithAGapInTheMiddle() {
        return List.of(
                new MonthlyTotals(2026, 3, new BigDecimal("430000.00"), new BigDecimal("298000.00")),
                new MonthlyTotals(2026, 4, new BigDecimal("440000.00"), new BigDecimal("300000.00")),
                new MonthlyTotals(2026, 6, new BigDecimal("450000.00"), new BigDecimal("310000.00")),
                new MonthlyTotals(2026, 7, new BigDecimal("460000.00"), new BigDecimal("320000.00")),
                new MonthlyTotals(2026, 8, new BigDecimal("470000.00"), new BigDecimal("330000.00")));
    }

    @Test
    void getCategories_ReturnsTheMonthTheTotalAndEveryCategoryRow() throws Exception {
        givenCategoryRows(populatedCategoryRows());

        mockMvc.perform(get("/api/reports/categories").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.month").value("2026-07"))
                .andExpect(jsonPath("$.total").value(300000.00))
                .andExpect(jsonPath("$.categories.length()").value(3))
                .andExpect(jsonPath("$.categories[0].categoryId").value(4))
                .andExpect(jsonPath("$.categories[0].categoryName").value("Lakhatás"))
                .andExpect(jsonPath("$.categories[0].total").value(150000.00))
                .andExpect(jsonPath("$.categories[0].percentage").value(50.0))
                .andExpect(jsonPath("$.categories[1].categoryId").value(1))
                .andExpect(jsonPath("$.categories[1].percentage").value(30.0))
                .andExpect(jsonPath("$.categories[2].categoryId").value(2))
                .andExpect(jsonPath("$.categories[2].percentage").value(20.0));
    }

    @Test
    void getCategories_MoneyAndPercentageGoOnTheWireAsJsonNumbers_NotStrings() throws Exception {
        givenCategoryRows(populatedCategoryRows());

        mockMvc.perform(get("/api/reports/categories").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.categories[0].total").isNumber())
                .andExpect(jsonPath("$.categories[0].percentage").isNumber())
                .andExpect(content().string(containsString("\"total\":300000.00")))
                .andExpect(content().string(containsString("\"total\":150000.00")))
                .andExpect(content().string(containsString("\"percentage\":50.0")));
    }

    @Test
    void getCategories_KeepsTheQueryOrderOnTheWire_SoTheDonutColoursStayStable() throws Exception {
        givenCategoryRows(List.of(
                new CategoryExpenseTotal(9L, "harmadik", new BigDecimal("500.00")),
                new CategoryExpenseTotal(2L, "első", new BigDecimal("500.00")),
                new CategoryExpenseTotal(7L, "második", new BigDecimal("100.00"))));

        mockMvc.perform(get("/api/reports/categories").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].categoryId").value(9))
                .andExpect(jsonPath("$.categories[1].categoryId").value(2))
                .andExpect(jsonPath("$.categories[2].categoryId").value(7));
    }

    @Test
    void getCategories_MonthWithoutExpenses_Is200WithZeroTotalAndAnEmptyArray() throws Exception {
        givenCategoryRows(List.of());

        mockMvc.perform(get("/api/reports/categories").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-07"))
                .andExpect(jsonPath("$.total").value(0.00))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(0))
                .andExpect(content().string(containsString("\"categories\":[]")));
    }

    @Test
    void getCategories_AnUndefinedShare_IsEmittedAsJsonNullNotOmittedFromTheBody() throws Exception {
        givenCategoryRows(List.of(new CategoryExpenseTotal(3L, "nullás", BigDecimal.ZERO)));

        mockMvc.perform(get("/api/reports/categories").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"percentage\":null")))
                .andExpect(jsonPath("$.categories[0].percentage").hasJsonPath())
                .andExpect(jsonPath("$.categories[0].percentage").value(nullValue()));
    }

    @Test
    void getCategories_WithoutMonthParameter_UsesTheServerCalendarMonth() throws Exception {
        givenCategoryRows(List.of());

        mockMvc.perform(get("/api/reports/categories").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(YearMonth.now().toString()));
    }

    @Test
    void getCategories_MalformedMonth_Returns400WithUnprefixedMonthFieldErrorAndNoQuery() throws Exception {
        mockMvc.perform(get("/api/reports/categories").param("month", "2026-07-01").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.month").exists())
                .andExpect(jsonPath("$.fieldErrors['getCategoryBreakdown.month']").doesNotExist());

        verify(transactionRepository, never()).summarizeExpensesByCategory(anyLong(), any(), any(), any());
    }

    @Test
    void getCategories_QueriesTheAuthenticatedUserForExpensesInsideTheRequestedMonth() throws Exception {
        givenCategoryRows(List.of());

        mockMvc.perform(get("/api/reports/categories").param("month", "2026-07").cookie(authCookie()))
                .andExpect(status().isOk());

        verify(transactionRepository).summarizeExpensesByCategory(
                eq(CURRENT_USER_ID),
                eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 7, 31)),
                eq(TransactionType.EXPENSE));
    }

    @Test
    void getCategories_IgnoresAUserIdQueryParameter_AndStillQueriesTheAuthenticatedUser() throws Exception {
        givenCategoryRows(List.of());

        mockMvc.perform(get("/api/reports/categories")
                .param("month", "2026-07")
                .param("userId", "2")
                .cookie(authCookie()))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> userId = ArgumentCaptor.forClass(Long.class);
        verify(transactionRepository).summarizeExpensesByCategory(userId.capture(), any(), any(), any());
        assertEquals(CURRENT_USER_ID, userId.getValue());
    }

    @Test
    void getCategories_WithoutAuthCookie_Returns401AndNeverQueries() throws Exception {
        mockMvc.perform(get("/api/reports/categories").param("month", "2026-07"))
                .andExpect(status().isUnauthorized());

        verify(transactionRepository, never()).summarizeExpensesByCategory(anyLong(), any(), any(), any());
    }

    @Test
    void getTrend_ReturnsTheRequestedMonthTheLengthAndTheWholeSeries() throws Exception {
        givenMonthlyRows(monthlyRowsWithAGapInTheMiddle());

        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "6")
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.month").value("2026-08"))
                .andExpect(jsonPath("$.months").value(6))
                .andExpect(jsonPath("$.points.length()").value(6))
                .andExpect(jsonPath("$.points[0].month").value("2026-03"))
                .andExpect(jsonPath("$.points[5].month").value("2026-08"))
                .andExpect(jsonPath("$.points[0].income").value(430000.00))
                .andExpect(jsonPath("$.points[5].expense").value(330000.00));
    }

    @Test
    void getTrend_AMonthWithoutTransactionsInTheMiddleIsAZeroPoint_NotAMissingPoint() throws Exception {
        givenMonthlyRows(monthlyRowsWithAGapInTheMiddle());

        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "6")
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(6))
                .andExpect(jsonPath("$.points[2].month").value("2026-05"))
                .andExpect(jsonPath("$.points[2].income").value(0.00))
                .andExpect(jsonPath("$.points[2].expense").value(0.00))
                .andExpect(jsonPath("$.points[3].month").value("2026-06"))
                .andExpect(jsonPath("$.points[3].income").value(450000.00))
                .andExpect(content().string(containsString("{\"month\":\"2026-05\",\"income\":0.00,\"expense\":0.00}")));
    }

    @Test
    void getTrend_MoneyGoesOnTheWireAsJsonNumbers_NotStrings() throws Exception {
        givenMonthlyRows(monthlyRowsWithAGapInTheMiddle());

        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "6")
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[0].income").isNumber())
                .andExpect(jsonPath("$.points[0].expense").isNumber())
                .andExpect(content().string(containsString("\"income\":430000.00")))
                .andExpect(content().string(containsString("\"expense\":298000.00")));
    }

    @Test
    void getTrend_WithoutMonthsParameter_DefaultsToSixPoints() throws Exception {
        givenMonthlyRows(List.of());

        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months").value(6))
                .andExpect(jsonPath("$.points.length()").value(6))
                .andExpect(jsonPath("$.points[0].month").value("2026-03"))
                .andExpect(jsonPath("$.points[5].month").value("2026-08"));
    }

    @Test
    void getTrend_WithoutMonthParameter_EndsTheSeriesWithTheServerCalendarMonth() throws Exception {
        givenMonthlyRows(List.of());
        YearMonth serverMonth = YearMonth.now();

        mockMvc.perform(get("/api/reports/trend").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(serverMonth.toString()))
                .andExpect(jsonPath("$.points[5].month").value(serverMonth.toString()))
                .andExpect(jsonPath("$.points[0].month").value(serverMonth.minusMonths(5).toString()));
    }

    @Test
    void getTrend_ForAUserWithoutTransactions_Is200WithAFullLengthAllZeroSeries() throws Exception {
        givenMonthlyRows(List.of());

        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "6")
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(6))
                .andExpect(jsonPath("$.points[0].income").value(0.00))
                .andExpect(jsonPath("$.points[3].income").value(0.00))
                .andExpect(jsonPath("$.points[5].expense").value(0.00));
    }

    @Test
    void getTrend_AcceptsTheWholeSupportedRange() throws Exception {
        givenMonthlyRows(List.of());

        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "1")
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(1))
                .andExpect(jsonPath("$.points[0].month").value("2026-08"));

        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "24")
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(24))
                .andExpect(jsonPath("$.points[0].month").value("2024-09"))
                .andExpect(jsonPath("$.points[23].month").value("2026-08"));
    }

    @Test
    void getTrend_MonthsBelowTheRange_Returns400WithAMonthsFieldErrorAndNoQuery() throws Exception {
        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "0")
                .cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.months").exists())
                .andExpect(jsonPath("$.fieldErrors['getTrend.months']").doesNotExist());

        verify(transactionRepository, never()).summarizeMonthlyTotals(anyLong(), any(), any(), any(), any());
    }

    @Test
    void getTrend_MonthsAboveTheRange_Returns400WithAMonthsFieldErrorAndNoQuery() throws Exception {
        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "25")
                .cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.months").exists());

        verify(transactionRepository, never()).summarizeMonthlyTotals(anyLong(), any(), any(), any(), any());
    }

    @Test
    void getTrend_NonNumericMonths_Returns400WithAMonthsFieldErrorAndNoQuery() throws Exception {
        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "six")
                .cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.months").exists());

        verify(transactionRepository, never()).summarizeMonthlyTotals(anyLong(), any(), any(), any(), any());
    }

    @Test
    void getTrend_MalformedMonth_Returns400WithUnprefixedMonthFieldErrorAndNoQuery() throws Exception {
        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08-01").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.month").exists())
                .andExpect(jsonPath("$.fieldErrors['getTrend.month']").doesNotExist());

        verify(transactionRepository, never()).summarizeMonthlyTotals(anyLong(), any(), any(), any(), any());
    }

    @Test
    void getTrend_QueriesTheAuthenticatedUserForTheWholeSeriesWindowInOneGo() throws Exception {
        givenMonthlyRows(List.of());

        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08").param("months", "6")
                .cookie(authCookie()))
                .andExpect(status().isOk());

        verify(transactionRepository).summarizeMonthlyTotals(
                eq(CURRENT_USER_ID),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 8, 31)),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE));
    }

    @Test
    void getTrend_IgnoresAUserIdQueryParameter_AndStillQueriesTheAuthenticatedUser() throws Exception {
        givenMonthlyRows(List.of());

        mockMvc.perform(get("/api/reports/trend")
                .param("month", "2026-08")
                .param("userId", "2")
                .cookie(authCookie()))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> userId = ArgumentCaptor.forClass(Long.class);
        verify(transactionRepository).summarizeMonthlyTotals(userId.capture(), any(), any(), any(), any());
        assertEquals(CURRENT_USER_ID, userId.getValue());
    }

    @Test
    void getTrend_WithoutAuthCookie_Returns401AndNeverQueries() throws Exception {
        mockMvc.perform(get("/api/reports/trend").param("month", "2026-08"))
                .andExpect(status().isUnauthorized());

        verify(transactionRepository, never()).summarizeMonthlyTotals(anyLong(), any(), any(), any(), any());
    }

    private void givenBudgetRows(List<CategoryBudgetTotal> rows) {
        when(budgetRepository.summarizeBudgetsByCategory(anyLong(), any(), any())).thenReturn(rows);
    }

    private static List<CategoryBudgetTotal> populatedBudgetRows() {
        return List.of(
                new CategoryBudgetTotal(4L, "Lakhatás", new BigDecimal("150000.00")),
                new CategoryBudgetTotal(1L, "Élelmiszer", new BigDecimal("100000.00")));
    }

    private static List<CategoryExpenseTotal> expenseRowsWithOneUnbudgetedCategory() {
        return List.of(
                new CategoryExpenseTotal(4L, "Lakhatás", new BigDecimal("162000.00")),
                new CategoryExpenseTotal(1L, "Élelmiszer", new BigDecimal("36000.00")),
                new CategoryExpenseTotal(9L, "Szórakozás", new BigDecimal("42000.00")));
    }

    @Test
    void getBudgetStatus_ReturnsTheMonthTheTotalsAndEveryBudgetedCategoryRow() throws Exception {
        givenBudgetRows(populatedBudgetRows());
        givenCategoryRows(expenseRowsWithOneUnbudgetedCategory());

        mockMvc.perform(get("/api/reports/budget-status").param("month", "2026-08").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.month").value("2026-08"))
                .andExpect(jsonPath("$.totalBudgeted").value(250000.00))
                .andExpect(jsonPath("$.totalSpent").value(198000.00))
                .andExpect(jsonPath("$.unbudgetedSpending").value(42000.00))
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.categories[0].categoryId").value(4))
                .andExpect(jsonPath("$.categories[0].categoryName").value("Lakhatás"))
                .andExpect(jsonPath("$.categories[0].budgeted").value(150000.00))
                .andExpect(jsonPath("$.categories[0].spent").value(162000.00))
                .andExpect(jsonPath("$.categories[0].remaining").value(-12000.00))
                .andExpect(jsonPath("$.categories[0].percentageUsed").value(108.0))
                .andExpect(jsonPath("$.categories[1].categoryId").value(1))
                .andExpect(jsonPath("$.categories[1].budgeted").value(100000.00))
                .andExpect(jsonPath("$.categories[1].spent").value(36000.00))
                .andExpect(jsonPath("$.categories[1].remaining").value(64000.00))
                .andExpect(jsonPath("$.categories[1].percentageUsed").value(36.0));
    }

    @Test
    void getBudgetStatus_MoneyAndPercentageGoOnTheWireAsJsonNumbers_NotStrings() throws Exception {
        givenBudgetRows(populatedBudgetRows());
        givenCategoryRows(expenseRowsWithOneUnbudgetedCategory());

        mockMvc.perform(get("/api/reports/budget-status").param("month", "2026-08").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBudgeted").isNumber())
                .andExpect(jsonPath("$.totalSpent").isNumber())
                .andExpect(jsonPath("$.unbudgetedSpending").isNumber())
                .andExpect(jsonPath("$.categories[0].budgeted").isNumber())
                .andExpect(jsonPath("$.categories[0].spent").isNumber())
                .andExpect(jsonPath("$.categories[0].remaining").isNumber())
                .andExpect(jsonPath("$.categories[0].percentageUsed").isNumber())
                .andExpect(content().string(containsString("\"totalBudgeted\":250000.00")))
                .andExpect(content().string(containsString("\"unbudgetedSpending\":42000.00")))
                .andExpect(content().string(containsString("\"budgeted\":150000.00")))
                .andExpect(content().string(containsString("\"percentageUsed\":108.0")));
    }

    @Test
    void getBudgetStatus_OverspendingKeepsTheNegativeSignOnTheWire_AndIsNotClampedToOneHundred() throws Exception {
        givenBudgetRows(populatedBudgetRows());
        givenCategoryRows(expenseRowsWithOneUnbudgetedCategory());

        mockMvc.perform(get("/api/reports/budget-status").param("month", "2026-08").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"remaining\":-12000.00")))
                .andExpect(content().string(containsString("\"percentageUsed\":108.0")));
    }

    @Test
    void getBudgetStatus_AnUndefinedPercentage_IsEmittedAsJsonNullNotOmittedFromTheBody() throws Exception {
        givenBudgetRows(List.of(new CategoryBudgetTotal(3L, "nullás keret", BigDecimal.ZERO)));
        givenCategoryRows(List.of(new CategoryExpenseTotal(3L, "nullás keret", new BigDecimal("5000.00"))));

        mockMvc.perform(get("/api/reports/budget-status").param("month", "2026-08").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"percentageUsed\":null")))
                .andExpect(jsonPath("$.categories[0].percentageUsed").hasJsonPath())
                .andExpect(jsonPath("$.categories[0].percentageUsed").value(nullValue()))
                .andExpect(jsonPath("$.categories[0].remaining").value(-5000.00));
    }

    @Test
    void getBudgetStatus_MonthWithoutBudgets_Is200WithAnEmptyArrayAndTheWholeMonthSpendAsUnbudgeted()
            throws Exception {
        givenBudgetRows(List.of());
        givenCategoryRows(expenseRowsWithOneUnbudgetedCategory());

        mockMvc.perform(get("/api/reports/budget-status").param("month", "2026-08").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBudgeted").value(0.00))
                .andExpect(jsonPath("$.totalSpent").value(0.00))
                .andExpect(jsonPath("$.unbudgetedSpending").value(240000.00))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(0))
                .andExpect(content().string(containsString("\"categories\":[]")))
                .andExpect(content().string(containsString("\"unbudgetedSpending\":240000.00")));
    }

    @Test
    void getBudgetStatus_WithoutMonthParameter_UsesTheServerCalendarMonth() throws Exception {
        givenBudgetRows(List.of());
        givenCategoryRows(List.of());

        mockMvc.perform(get("/api/reports/budget-status").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(YearMonth.now().toString()));
    }

    @Test
    void getBudgetStatus_MalformedMonth_Returns400WithUnprefixedMonthFieldErrorAndNoQuery() throws Exception {
        mockMvc.perform(get("/api/reports/budget-status").param("month", "2026-08-01").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.month").exists())
                .andExpect(jsonPath("$.fieldErrors['getBudgetStatus.month']").doesNotExist());

        verify(budgetRepository, never()).summarizeBudgetsByCategory(anyLong(), any(), any());
        verify(transactionRepository, never()).summarizeExpensesByCategory(anyLong(), any(), any(), any());
    }

    @Test
    void getBudgetStatus_QueriesBothTablesForTheAuthenticatedUserInsideTheRequestedMonth() throws Exception {
        givenBudgetRows(List.of());
        givenCategoryRows(List.of());

        mockMvc.perform(get("/api/reports/budget-status").param("month", "2026-08").cookie(authCookie()))
                .andExpect(status().isOk());

        verify(budgetRepository).summarizeBudgetsByCategory(
                eq(CURRENT_USER_ID),
                eq(LocalDate.of(2026, 8, 1)),
                eq(LocalDate.of(2026, 8, 31)));
        verify(transactionRepository).summarizeExpensesByCategory(
                eq(CURRENT_USER_ID),
                eq(LocalDate.of(2026, 8, 1)),
                eq(LocalDate.of(2026, 8, 31)),
                eq(TransactionType.EXPENSE));
    }

    @Test
    void getBudgetStatus_IgnoresAUserIdQueryParameter_OnBothTables() throws Exception {
        givenBudgetRows(List.of());
        givenCategoryRows(List.of());

        mockMvc.perform(get("/api/reports/budget-status")
                .param("month", "2026-08")
                .param("userId", "2")
                .cookie(authCookie()))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> budgetUserId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> expenseUserId = ArgumentCaptor.forClass(Long.class);
        verify(budgetRepository).summarizeBudgetsByCategory(budgetUserId.capture(), any(), any());
        verify(transactionRepository).summarizeExpensesByCategory(expenseUserId.capture(), any(), any(), any());
        assertEquals(CURRENT_USER_ID, budgetUserId.getValue());
        assertEquals(CURRENT_USER_ID, expenseUserId.getValue());
    }

    @Test
    void getBudgetStatus_WithoutAuthCookie_Returns401AndNeverQueriesEitherTable() throws Exception {
        mockMvc.perform(get("/api/reports/budget-status").param("month", "2026-08"))
                .andExpect(status().isUnauthorized());

        verify(budgetRepository, never()).summarizeBudgetsByCategory(anyLong(), any(), any());
        verify(transactionRepository, never()).summarizeExpensesByCategory(anyLong(), any(), any(), any());
    }
}
