package hu.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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

import hu.financial.dto.report.BudgetStatusItemDto;
import hu.financial.dto.report.BudgetStatusResponseDto;
import hu.financial.dto.report.ReportPeriod;
import hu.financial.model.Budget;
import hu.financial.model.Category;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.BudgetRepository;
import hu.financial.repository.CategoryRepository;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.UserRepository;
import hu.financial.service.ReportService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ReportBudgetStatusIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15"));

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final AtomicLong SEEDS = new AtomicLong();

    private static final String SHARED_CATEGORY_NAME = "Élelmiszer";

    private static final String REQUESTED_MONTH = "2026-07";

    private static final String MONTH_WITHOUT_BUDGETS = "2026-08";

    private static final String MONTH_WITHOUT_SPENDING = "2026-09";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ReportService reportService;

    private User owner;

    private User stranger;

    private User newcomer;

    private Category ownerHome;

    private Category ownerFood;

    private Category ownerTravel;

    private Category ownerFun;

    private Category ownerSaving;

    private Category ownerClothes;

    private Category ownerHealth;

    private Category strangerHome;

    private Category strangerFood;

    @BeforeEach
    void seedTwoUsersWhoseBudgetsAndCategoriesShareAName() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        String run = Long.toString(SEEDS.incrementAndGet());

        owner = userRepository.saveAndFlush(
                new User("statusowner" + run, "encoded-password", "statusowner" + run + "@example.com"));
        stranger = userRepository.saveAndFlush(
                new User("statusstranger" + run, "encoded-password", "statusstranger" + run + "@example.com"));
        newcomer = userRepository.saveAndFlush(
                new User("statusnewcomer" + run, "encoded-password", "statusnewcomer" + run + "@example.com"));

        ownerHome = categoryRepository.saveAndFlush(new Category("Lakhatás", "owner home", owner));
        ownerFood = categoryRepository.saveAndFlush(new Category(SHARED_CATEGORY_NAME, "owner food", owner));
        ownerTravel = categoryRepository.saveAndFlush(new Category("Utazás", "owner travel", owner));
        ownerFun = categoryRepository.saveAndFlush(new Category("Szórakozás", "owner fun", owner));
        ownerSaving = categoryRepository.saveAndFlush(new Category("Megtakarítás", "owner saving", owner));
        ownerClothes = categoryRepository.saveAndFlush(new Category("Ruházat", "owner clothes", owner));
        ownerHealth = categoryRepository.saveAndFlush(new Category("Egészség", "owner health", owner));

        strangerHome = categoryRepository.saveAndFlush(new Category("Lakhatás", "stranger home", stranger));
        strangerFood = categoryRepository.saveAndFlush(new Category(SHARED_CATEGORY_NAME, "stranger food", stranger));

        budget(owner, ownerHome, LocalDate.of(2026, 7, 1), "150000.00");
        budget(owner, ownerFood, LocalDate.of(2026, 7, 20), "100000.00");
        budget(owner, ownerTravel, LocalDate.of(2026, 7, 15), "0.00");
        budget(owner, ownerSaving, LocalDate.of(2026, 7, 1), "30000.00");
        budget(owner, ownerClothes, LocalDate.of(2026, 7, 1), "10000.00");
        budget(owner, ownerHealth, LocalDate.of(2026, 7, 1), "20000.00");
        budget(owner, ownerHome, LocalDate.of(2026, 9, 1), "999999.00");

        budget(stranger, strangerFood, LocalDate.of(2026, 7, 1), "500000.00");

        expense(owner, ownerHome, LocalDate.of(2026, 7, 1), "162000.00");
        income(owner, ownerHome, LocalDate.of(2026, 7, 5), "500000.00");
        expense(owner, ownerFood, LocalDate.of(2026, 7, 15), "36000.00");
        income(owner, ownerFood, LocalDate.of(2026, 7, 16), "20000.00");
        expense(owner, ownerTravel, LocalDate.of(2026, 7, 10), "5000.00");
        expense(owner, ownerFun, LocalDate.of(2026, 7, 12), "42000.00");
        expense(owner, ownerClothes, LocalDate.of(2026, 7, 3), "5000.00");
        expense(owner, ownerHealth, LocalDate.of(2026, 7, 4), "10000.00");
        expense(owner, ownerHome, LocalDate.of(2026, 8, 1), "888888.00");

        expense(stranger, strangerFood, LocalDate.of(2026, 7, 10), "7000.00");
        expense(stranger, strangerHome, LocalDate.of(2026, 7, 11), "3000.00");
    }

    private void budget(User user, Category category, LocalDate month, String amount) {
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(month);
        budget.setAmount(new BigDecimal(amount));
        budgetRepository.saveAndFlush(budget);
    }

    private void expense(User user, Category category, LocalDate date, String amount) {
        persist(user, category, TransactionType.EXPENSE, date, amount);
    }

    private void income(User user, Category category, LocalDate date, String amount) {
        persist(user, category, TransactionType.INCOME, date, amount);
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

    private BudgetStatusResponseDto statusFor(User user, String month) {
        return reportService.budgetStatus(user.getId(), ReportPeriod.of(month));
    }

    private static List<Long> categoryIds(BudgetStatusResponseDto status) {
        return status.categories().stream().map(BudgetStatusItemDto::categoryId).toList();
    }

    private static BudgetStatusItemDto rowOf(BudgetStatusResponseDto status, Category category) {
        return status.categories().stream()
                .filter(item -> category.getId().equals(item.categoryId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void bothUsersRowsAreReallyInTheTables_SoTheIsolationAssertionsBelowAreNotVacuous() {
        assertEquals(8, budgetRepository.count());
        assertEquals(11, transactionRepository.count());
        assertEquals(SHARED_CATEGORY_NAME, ownerFood.getName());
        assertEquals(SHARED_CATEGORY_NAME, strangerFood.getName());
        assertNotEquals(ownerFood.getId(), strangerFood.getId());
    }

    @Test
    void budgetStatus_ForTheOwner_CarriesEveryBudgetedCategoryOfTheMonthWithItsTotals() {
        BudgetStatusResponseDto status = statusFor(owner, REQUESTED_MONTH);

        assertEquals("2026-07", status.month());
        assertEquals(6, status.categories().size());
        assertEquals(new BigDecimal("310000.00"), status.totalBudgeted());
        assertEquals(new BigDecimal("218000.00"), status.totalSpent());
        assertEquals(new BigDecimal("42000.00"), status.unbudgetedSpending());
    }

    @Test
    void budgetStatus_TheSecondBudgetOfTheMonthCountsEvenThoughItsDayComponentIsNotTheFirst() {
        BudgetStatusResponseDto status = statusFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("100000.00"), rowOf(status, ownerFood).budgeted());
        assertEquals(new BigDecimal("64000.00"), rowOf(status, ownerFood).remaining());
    }

    @Test
    void budgetStatus_Overspending_IsANegativeRemainingAndAPercentageAboveOneHundred() {
        BudgetStatusItemDto home = rowOf(statusFor(owner, REQUESTED_MONTH), ownerHome);

        assertEquals(new BigDecimal("150000.00"), home.budgeted());
        assertEquals(new BigDecimal("162000.00"), home.spent());
        assertEquals(new BigDecimal("-12000.00"), home.remaining());
        assertNotEquals(new BigDecimal("12000.00"), home.remaining());
        assertEquals(new BigDecimal("108.0"), home.percentageUsed());
    }

    @Test
    void budgetStatus_IncomeInABudgetedCategory_DoesNotReduceTheSpentAmount() {
        BudgetStatusResponseDto status = statusFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("162000.00"), rowOf(status, ownerHome).spent());
        assertEquals(new BigDecimal("36000.00"), rowOf(status, ownerFood).spent());
        assertNotEquals(new BigDecimal("16000.00"), rowOf(status, ownerFood).spent());
        assertEquals(new BigDecimal("36.0"), rowOf(status, ownerFood).percentageUsed());
    }

    @Test
    void budgetStatus_BudgetWithoutSpending_IsStillARowWithZeroSpentAndZeroPercentage() {
        BudgetStatusItemDto saving = rowOf(statusFor(owner, REQUESTED_MONTH), ownerSaving);

        assertEquals(new BigDecimal("30000.00"), saving.budgeted());
        assertEquals(new BigDecimal("0.00"), saving.spent());
        assertEquals(new BigDecimal("30000.00"), saving.remaining());
        assertEquals(new BigDecimal("0.0"), saving.percentageUsed());
    }

    @Test
    void budgetStatus_ZeroBudget_YieldsNullPercentageUsedAndStillReportsTheSpending() {
        BudgetStatusItemDto travel = rowOf(statusFor(owner, REQUESTED_MONTH), ownerTravel);

        assertEquals(new BigDecimal("0.00"), travel.budgeted());
        assertEquals(new BigDecimal("5000.00"), travel.spent());
        assertEquals(new BigDecimal("-5000.00"), travel.remaining());
        assertNull(travel.percentageUsed());
    }

    @Test
    void budgetStatus_SpendingInACategoryWithoutABudget_IsNotARowButFeedsUnbudgetedSpending() {
        BudgetStatusResponseDto status = statusFor(owner, REQUESTED_MONTH);

        assertFalse(categoryIds(status).contains(ownerFun.getId()));
        assertEquals(new BigDecimal("42000.00"), status.unbudgetedSpending());
        assertEquals(new BigDecimal("218000.00"), status.totalSpent());
        assertNotEquals(new BigDecimal("260000.00"), status.totalSpent());
    }

    @Test
    void budgetStatus_OrdersByPercentageUsedDescendingWithTheUndefinedOneLast() {
        BudgetStatusResponseDto status = statusFor(owner, REQUESTED_MONTH);

        assertEquals(List.of(
                ownerHome.getId(),
                ownerClothes.getId(),
                ownerHealth.getId(),
                ownerFood.getId(),
                ownerSaving.getId(),
                ownerTravel.getId()), categoryIds(status));
        assertEquals(new BigDecimal("108.0"), status.categories().get(0).percentageUsed());
        assertEquals(new BigDecimal("36.0"), status.categories().get(3).percentageUsed());
        assertEquals(new BigDecimal("0.0"), status.categories().get(4).percentageUsed());
        assertNull(status.categories().get(5).percentageUsed());
    }

    @Test
    void budgetStatus_BreaksEqualPercentagesByAscendingCategoryId() {
        BudgetStatusResponseDto status = statusFor(owner, REQUESTED_MONTH);

        BudgetStatusItemDto first = status.categories().get(1);
        BudgetStatusItemDto second = status.categories().get(2);

        assertEquals(new BigDecimal("50.0"), first.percentageUsed());
        assertEquals(new BigDecimal("50.0"), second.percentageUsed());
        assertTrue(first.categoryId() < second.categoryId());
        assertEquals(Set.of(ownerClothes.getId(), ownerHealth.getId()),
                Set.of(first.categoryId(), second.categoryId()));
    }

    @Test
    void budgetStatus_RepeatedCallsReturnTheSameOrder() {
        List<Long> first = categoryIds(statusFor(owner, REQUESTED_MONTH));
        List<Long> second = categoryIds(statusFor(owner, REQUESTED_MONTH));
        List<Long> third = categoryIds(statusFor(owner, REQUESTED_MONTH));

        assertEquals(first, second);
        assertEquals(second, third);
    }

    @Test
    void budgetStatus_TheStrangersBudgetOnTheSameCategoryName_NeverInflatesTheOwnersBudgetedAmount() {
        BudgetStatusResponseDto status = statusFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("100000.00"), rowOf(status, ownerFood).budgeted());
        assertNotEquals(new BigDecimal("600000.00"), rowOf(status, ownerFood).budgeted());
        assertEquals(new BigDecimal("310000.00"), status.totalBudgeted());
        assertNotEquals(new BigDecimal("810000.00"), status.totalBudgeted());
    }

    @Test
    void budgetStatus_TheStrangersSpendingOnTheSameCategoryName_NeverInflatesTheOwnersSpentAmount() {
        BudgetStatusResponseDto status = statusFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("36000.00"), rowOf(status, ownerFood).spent());
        assertNotEquals(new BigDecimal("43000.00"), rowOf(status, ownerFood).spent());
        assertEquals(new BigDecimal("42000.00"), status.unbudgetedSpending());
        assertNotEquals(new BigDecimal("45000.00"), status.unbudgetedSpending());
    }

    @Test
    void budgetStatus_ForTheOwner_NeverCarriesACategoryThatBelongsToTheOtherUser() {
        Set<Long> strangerCategoryIds = Set.of(strangerHome.getId(), strangerFood.getId());

        List<Long> ownerCategoryIds = categoryIds(statusFor(owner, REQUESTED_MONTH));

        assertFalse(ownerCategoryIds.stream().anyMatch(strangerCategoryIds::contains));
    }

    @Test
    void budgetStatus_ForTheStranger_IsComputedFromThatUsersOwnBudgetsAndOwnTransactions() {
        BudgetStatusResponseDto status = statusFor(stranger, REQUESTED_MONTH);

        assertEquals(List.of(strangerFood.getId()), categoryIds(status));
        assertEquals(new BigDecimal("500000.00"), status.totalBudgeted());
        assertEquals(new BigDecimal("7000.00"), status.totalSpent());
        assertEquals(new BigDecimal("3000.00"), status.unbudgetedSpending());
        assertEquals(new BigDecimal("1.4"), status.categories().get(0).percentageUsed());
        assertEquals(new BigDecimal("493000.00"), status.categories().get(0).remaining());
    }

    @Test
    void budgetStatus_MonthWithoutBudgets_IsAnEmptyListWhoseUnbudgetedSpendingIsTheWholeMonthSpend() {
        BudgetStatusResponseDto status = statusFor(owner, MONTH_WITHOUT_BUDGETS);

        assertEquals("2026-08", status.month());
        assertTrue(status.categories().isEmpty());
        assertEquals(new BigDecimal("0.00"), status.totalBudgeted());
        assertEquals(new BigDecimal("0.00"), status.totalSpent());
        assertEquals(new BigDecimal("888888.00"), status.unbudgetedSpending());
        assertNotEquals(new BigDecimal("0.00"), status.unbudgetedSpending());
    }

    @Test
    void budgetStatus_MonthWithABudgetButNoSpending_IsAZeroPercentRowAndNoUnbudgetedSpending() {
        BudgetStatusResponseDto status = statusFor(owner, MONTH_WITHOUT_SPENDING);

        assertEquals(List.of(ownerHome.getId()), categoryIds(status));
        assertEquals(new BigDecimal("999999.00"), status.totalBudgeted());
        assertEquals(new BigDecimal("0.00"), status.totalSpent());
        assertEquals(new BigDecimal("0.00"), status.unbudgetedSpending());
        assertEquals(new BigDecimal("0.0"), status.categories().get(0).percentageUsed());
    }

    @Test
    void budgetStatus_NeighbouringMonthsBudgetsAreExcluded() {
        BudgetStatusResponseDto july = statusFor(owner, REQUESTED_MONTH);
        BudgetStatusResponseDto september = statusFor(owner, MONTH_WITHOUT_SPENDING);

        assertFalse(july.categories().stream()
                .anyMatch(item -> new BigDecimal("999999.00").equals(item.budgeted())));
        assertEquals(new BigDecimal("999999.00"), september.categories().get(0).budgeted());
        assertEquals(1, september.categories().size());
    }

    @Test
    void budgetStatus_ForAUserWithoutBudgetsOrTransactions_IsAllZerosAndAnEmptyList() {
        BudgetStatusResponseDto status = statusFor(newcomer, REQUESTED_MONTH);

        assertTrue(status.categories().isEmpty());
        assertEquals(new BigDecimal("0.00"), status.totalBudgeted());
        assertEquals(new BigDecimal("0.00"), status.totalSpent());
        assertEquals(new BigDecimal("0.00"), status.unbudgetedSpending());
    }
}
