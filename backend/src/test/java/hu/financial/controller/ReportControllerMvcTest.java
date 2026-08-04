package hu.financial.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import hu.financial.config.SecurityConfig;
import hu.financial.filter.JwtAuthenticationFilter;
import hu.financial.model.User;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.TransactionRepository;
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
        SecurityCookieFactory.class, CsrfCookieFilter.class, JwtService.class, ReportService.class })
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

    @MockBean
    private UserService userService;

    @MockBean
    private TransactionRepository transactionRepository;

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
}
