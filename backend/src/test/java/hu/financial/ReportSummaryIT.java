package hu.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import hu.financial.dto.report.ReportPeriod;
import hu.financial.dto.report.SummaryResponseDto;
import hu.financial.model.Category;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.CategoryRepository;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.UserRepository;
import hu.financial.service.ReportService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ReportSummaryIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15"));

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final AtomicLong SEEDS = new AtomicLong();

    private static final String REQUESTED_MONTH = "2026-07";

    private static final String FIRST_ACTIVE_MONTH = "2026-05";

    private static final String FUTURE_MONTH = "2026-12";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ReportService reportService;

    private User owner;

    private User stranger;

    private User newcomer;

    @BeforeEach
    void seedTwoUsersWithOverlappingMonths() {
        transactionRepository.deleteAll();
        String run = Long.toString(SEEDS.incrementAndGet());

        owner = userRepository.saveAndFlush(
                new User("reportowner" + run, "encoded-password", "reportowner" + run + "@example.com"));
        stranger = userRepository.saveAndFlush(
                new User("reportstranger" + run, "encoded-password", "reportstranger" + run + "@example.com"));
        newcomer = userRepository.saveAndFlush(
                new User("reportnewcomer" + run, "encoded-password", "reportnewcomer" + run + "@example.com"));

        Category ownerCategory = categoryRepository.saveAndFlush(new Category("owner budget", "own", owner));
        Category strangerCategory = categoryRepository
                .saveAndFlush(new Category("stranger budget", "not yours", stranger));

        persist(owner, ownerCategory, TransactionType.INCOME, LocalDate.of(2026, 5, 10), "1000.00");
        persist(owner, ownerCategory, TransactionType.INCOME, LocalDate.of(2026, 6, 1), "400.00");
        persist(owner, ownerCategory, TransactionType.EXPENSE, LocalDate.of(2026, 6, 30), "100.00");
        persist(owner, ownerCategory, TransactionType.INCOME, LocalDate.of(2026, 7, 1), "500.00");
        persist(owner, ownerCategory, TransactionType.EXPENSE, LocalDate.of(2026, 7, 31), "200.00");
        persist(owner, ownerCategory, TransactionType.INCOME, LocalDate.of(2026, 8, 1), "9999.00");

        persist(stranger, strangerCategory, TransactionType.INCOME, LocalDate.of(2026, 5, 1), "100000.00");
        persist(stranger, strangerCategory, TransactionType.INCOME, LocalDate.of(2026, 6, 2), "50000.00");
        persist(stranger, strangerCategory, TransactionType.EXPENSE, LocalDate.of(2026, 6, 3), "20000.00");
        persist(stranger, strangerCategory, TransactionType.INCOME, LocalDate.of(2026, 7, 2), "70000.00");
        persist(stranger, strangerCategory, TransactionType.EXPENSE, LocalDate.of(2026, 7, 3), "30000.00");
    }

    private void persist(User user, Category category, TransactionType type, LocalDate date, String amount) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setDescription(user.getUsername() + "-" + date);
        transaction.setCategory(category);
        transaction.setUser(user);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDate(date);
        transactionRepository.saveAndFlush(transaction);
    }

    private SummaryResponseDto summaryFor(User user, String month) {
        return reportService.summarize(user.getId(), ReportPeriod.of(month));
    }

    @Test
    void bothUsersRowsAreReallyInTheTable_SoTheIsolationAssertionsBelowAreNotVacuous() {
        assertEquals(11, transactionRepository.count());
    }

    @Test
    void summary_ForTheOwner_CountsOnlyTheOwnersMonthlyIncomeAndExpense() {
        SummaryResponseDto summary = summaryFor(owner, REQUESTED_MONTH);

        assertEquals("2026-07", summary.month());
        assertEquals("2026-06", summary.previousMonth());
        assertEquals(new BigDecimal("500.00"), summary.income().current());
        assertEquals(new BigDecimal("400.00"), summary.income().previous());
        assertEquals(new BigDecimal("25.0"), summary.income().deltaPercent());
        assertEquals(new BigDecimal("200.00"), summary.expense().current());
        assertEquals(new BigDecimal("100.00"), summary.expense().previous());
        assertEquals(new BigDecimal("100.0"), summary.expense().deltaPercent());
    }

    @Test
    void summary_ForTheOwner_AccumulatesTheBalanceUpToTheMonthEndOnly() {
        SummaryResponseDto summary = summaryFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("1600.00"), summary.balance().current());
        assertEquals(new BigDecimal("1300.00"), summary.balance().previous());
        assertEquals(new BigDecimal("23.1"), summary.balance().deltaPercent());
    }

    @Test
    void summary_ForTheOwner_ComputesTheSavingsRateInPercentagePoints() {
        SummaryResponseDto summary = summaryFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("60.0"), summary.savingsRate().current());
        assertEquals(new BigDecimal("75.0"), summary.savingsRate().previous());
        assertEquals(new BigDecimal("-15.0"), summary.savingsRate().deltaPoints());
    }

    @Test
    void summary_ForTheStranger_IsComputedFromThatUsersOwnRowsOnly() {
        SummaryResponseDto summary = summaryFor(stranger, REQUESTED_MONTH);

        assertEquals(new BigDecimal("70000.00"), summary.income().current());
        assertEquals(new BigDecimal("50000.00"), summary.income().previous());
        assertEquals(new BigDecimal("30000.00"), summary.expense().current());
        assertEquals(new BigDecimal("20000.00"), summary.expense().previous());
        assertEquals(new BigDecimal("170000.00"), summary.balance().current());
        assertEquals(new BigDecimal("130000.00"), summary.balance().previous());
        assertEquals(new BigDecimal("57.1"), summary.savingsRate().current());
        assertEquals(new BigDecimal("60.0"), summary.savingsRate().previous());
    }

    @Test
    void summary_BalanceHasNoLowerDateBound_SoAMissingUserFilterWouldShowUpHereFirst() {
        BigDecimal ownerBalance = summaryFor(owner, REQUESTED_MONTH).balance().current();
        BigDecimal strangerBalance = summaryFor(stranger, REQUESTED_MONTH).balance().current();
        BigDecimal everyRowInTheTable = ownerBalance.add(strangerBalance);

        assertEquals(new BigDecimal("1600.00"), ownerBalance);
        assertEquals(new BigDecimal("170000.00"), strangerBalance);
        assertNotEquals(everyRowInTheTable, ownerBalance);
        assertNotEquals(everyRowInTheTable, strangerBalance);
    }

    @Test
    void summary_MonthBoundariesAreInclusiveOnBothEnds() {
        SummaryResponseDto summary = summaryFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("500.00"), summary.income().current());
        assertEquals(new BigDecimal("200.00"), summary.expense().current());
        assertEquals(new BigDecimal("400.00"), summary.income().previous());
        assertEquals(new BigDecimal("100.00"), summary.expense().previous());
    }

    @Test
    void summary_ExcludesTransactionsDatedAfterTheRequestedMonth() {
        SummaryResponseDto july = summaryFor(owner, REQUESTED_MONTH);
        SummaryResponseDto august = summaryFor(owner, "2026-08");

        assertEquals(new BigDecimal("1600.00"), july.balance().current());
        assertEquals(new BigDecimal("11599.00"), august.balance().current());
        assertEquals(new BigDecimal("1600.00"), august.balance().previous());
    }

    @Test
    void summary_ForTheFirstActiveMonth_HasNullDeltasBecauseThePreviousMonthIsEmpty() {
        SummaryResponseDto summary = summaryFor(owner, FIRST_ACTIVE_MONTH);

        assertEquals(new BigDecimal("1000.00"), summary.income().current());
        assertEquals(new BigDecimal("0.00"), summary.income().previous());
        assertNull(summary.income().deltaPercent());
        assertEquals(new BigDecimal("0.00"), summary.expense().current());
        assertNull(summary.expense().deltaPercent());
        assertEquals(new BigDecimal("1000.00"), summary.balance().current());
        assertEquals(new BigDecimal("0.00"), summary.balance().previous());
        assertNull(summary.balance().deltaPercent());
        assertEquals(new BigDecimal("100.0"), summary.savingsRate().current());
        assertNull(summary.savingsRate().previous());
        assertNull(summary.savingsRate().deltaPoints());
    }

    @Test
    void summary_ForAUserWithoutAnyTransactions_IsZeroAndNullRatherThanAFailure() {
        SummaryResponseDto summary = summaryFor(newcomer, REQUESTED_MONTH);

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
    void summary_ForAFutureMonth_ReturnsZeroFlowAndTheFullAccumulatedBalance() {
        SummaryResponseDto summary = summaryFor(owner, FUTURE_MONTH);

        assertEquals("2026-12", summary.month());
        assertEquals("2026-11", summary.previousMonth());
        assertEquals(new BigDecimal("0.00"), summary.income().current());
        assertEquals(new BigDecimal("0.00"), summary.expense().current());
        assertEquals(new BigDecimal("11599.00"), summary.balance().current());
        assertEquals(new BigDecimal("11599.00"), summary.balance().previous());
        assertNull(summary.savingsRate().current());
        assertNull(summary.savingsRate().deltaPoints());
    }
}
