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

import hu.financial.dto.report.CategoryBreakdownItemDto;
import hu.financial.dto.report.CategoryBreakdownResponseDto;
import hu.financial.dto.report.ReportPeriod;
import hu.financial.dto.report.TrendPeriod;
import hu.financial.dto.report.TrendResponseDto;
import hu.financial.model.Category;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.CategoryRepository;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.UserRepository;
import hu.financial.service.ReportService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ReportBreakdownIT {

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

    private static final String LAST_TREND_MONTH = "2026-08";

    private static final String GAP_MONTH = "2026-05";

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

    private Category ownerHome;

    private Category ownerFood;

    private Category ownerTravel;

    private Category ownerLeisure;

    private Category strangerHome;

    private Category strangerFood;

    @BeforeEach
    void seedTwoUsersWhoseCategoriesShareAName() {
        transactionRepository.deleteAll();
        String run = Long.toString(SEEDS.incrementAndGet());

        owner = userRepository.saveAndFlush(
                new User("breakdownowner" + run, "encoded-password", "breakdownowner" + run + "@example.com"));
        stranger = userRepository.saveAndFlush(
                new User("breakdownstranger" + run, "encoded-password", "breakdownstranger" + run + "@example.com"));
        newcomer = userRepository.saveAndFlush(
                new User("breakdownnewcomer" + run, "encoded-password", "breakdownnewcomer" + run + "@example.com"));

        ownerHome = categoryRepository.saveAndFlush(new Category("Lakhatás", "owner home", owner));
        ownerFood = categoryRepository.saveAndFlush(new Category(SHARED_CATEGORY_NAME, "owner food", owner));
        ownerTravel = categoryRepository.saveAndFlush(new Category("Utazás", "owner travel", owner));
        ownerLeisure = categoryRepository.saveAndFlush(new Category("Szórakozás", "owner leisure", owner));

        strangerHome = categoryRepository.saveAndFlush(new Category("Lakhatás", "stranger home", stranger));
        strangerFood = categoryRepository.saveAndFlush(new Category(SHARED_CATEGORY_NAME, "stranger food", stranger));

        persist(owner, ownerHome, TransactionType.INCOME, LocalDate.of(2026, 3, 5), "430000.00");
        persist(owner, ownerHome, TransactionType.EXPENSE, LocalDate.of(2026, 3, 20), "298000.00");
        persist(owner, ownerHome, TransactionType.INCOME, LocalDate.of(2026, 4, 5), "440000.00");
        persist(owner, ownerHome, TransactionType.EXPENSE, LocalDate.of(2026, 4, 20), "300000.00");
        persist(owner, ownerHome, TransactionType.EXPENSE, LocalDate.of(2026, 6, 30), "100000.00");
        persist(owner, ownerHome, TransactionType.EXPENSE, LocalDate.of(2026, 7, 1), "150000.00");
        persist(owner, ownerHome, TransactionType.INCOME, LocalDate.of(2026, 7, 5), "500000.00");
        persist(owner, ownerFood, TransactionType.EXPENSE, LocalDate.of(2026, 7, 15), "60000.00");
        persist(owner, ownerFood, TransactionType.EXPENSE, LocalDate.of(2026, 7, 31), "30000.00");
        persist(owner, ownerTravel, TransactionType.EXPENSE, LocalDate.of(2026, 7, 10), "30000.00");
        persist(owner, ownerLeisure, TransactionType.EXPENSE, LocalDate.of(2026, 7, 10), "30000.00");
        persist(owner, ownerHome, TransactionType.EXPENSE, LocalDate.of(2026, 8, 1), "888888.00");

        persist(stranger, strangerHome, TransactionType.INCOME, LocalDate.of(2026, 5, 10), "1000000.00");
        persist(stranger, strangerFood, TransactionType.EXPENSE, LocalDate.of(2026, 7, 10), "7000.00");
        persist(stranger, strangerHome, TransactionType.EXPENSE, LocalDate.of(2026, 7, 11), "3000.00");
        persist(stranger, strangerHome, TransactionType.INCOME, LocalDate.of(2026, 8, 2), "2000.00");
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

    private CategoryBreakdownResponseDto breakdownFor(User user, String month) {
        return reportService.breakdownExpensesByCategory(user.getId(), ReportPeriod.of(month));
    }

    private TrendResponseDto trendFor(User user, String month, int months) {
        return reportService.trend(user.getId(), TrendPeriod.of(month, months));
    }

    private static List<Long> categoryIds(CategoryBreakdownResponseDto breakdown) {
        return breakdown.categories().stream().map(CategoryBreakdownItemDto::categoryId).toList();
    }

    private static List<String> pointMonths(TrendResponseDto trend) {
        return trend.points().stream().map(point -> point.month()).toList();
    }

    @Test
    void bothUsersRowsAreReallyInTheTable_SoTheIsolationAssertionsBelowAreNotVacuous() {
        assertEquals(16, transactionRepository.count());
        assertEquals(SHARED_CATEGORY_NAME, ownerFood.getName());
        assertEquals(SHARED_CATEGORY_NAME, strangerFood.getName());
        assertNotEquals(ownerFood.getId(), strangerFood.getId());
    }

    @Test
    void breakdown_ForTheOwner_ListsOnlyTheOwnersExpenseCategories() {
        CategoryBreakdownResponseDto breakdown = breakdownFor(owner, REQUESTED_MONTH);

        assertEquals("2026-07", breakdown.month());
        assertEquals(new BigDecimal("300000.00"), breakdown.total());
        assertEquals(List.of(ownerHome.getId(), ownerFood.getId(), ownerTravel.getId(), ownerLeisure.getId()),
                categoryIds(breakdown));
    }

    @Test
    void breakdown_GroupsByCategoryId_SoTwoUsersCategoriesWithTheSameNameAreNeverMerged() {
        CategoryBreakdownResponseDto ownerBreakdown = breakdownFor(owner, REQUESTED_MONTH);
        CategoryBreakdownResponseDto strangerBreakdown = breakdownFor(stranger, REQUESTED_MONTH);

        CategoryBreakdownItemDto ownersFood = ownerBreakdown.categories().stream()
                .filter(category -> SHARED_CATEGORY_NAME.equals(category.categoryName()))
                .findFirst()
                .orElseThrow();
        CategoryBreakdownItemDto strangersFood = strangerBreakdown.categories().stream()
                .filter(category -> SHARED_CATEGORY_NAME.equals(category.categoryName()))
                .findFirst()
                .orElseThrow();

        assertEquals(ownerFood.getId(), ownersFood.categoryId());
        assertEquals(strangerFood.getId(), strangersFood.categoryId());
        assertEquals(new BigDecimal("90000.00"), ownersFood.total());
        assertEquals(new BigDecimal("7000.00"), strangersFood.total());
        assertNotEquals(new BigDecimal("97000.00"), ownersFood.total());
        assertNotEquals(new BigDecimal("97000.00"), strangersFood.total());
    }

    @Test
    void breakdown_ForTheOwner_NeverCarriesACategoryThatBelongsToTheOtherUser() {
        Set<Long> strangerCategoryIds = Set.of(strangerHome.getId(), strangerFood.getId());

        List<Long> ownerCategoryIds = categoryIds(breakdownFor(owner, REQUESTED_MONTH));

        assertFalse(ownerCategoryIds.stream().anyMatch(strangerCategoryIds::contains));
    }

    @Test
    void breakdown_ForTheStranger_IsComputedFromThatUsersOwnRowsOnly() {
        CategoryBreakdownResponseDto breakdown = breakdownFor(stranger, REQUESTED_MONTH);

        assertEquals(new BigDecimal("10000.00"), breakdown.total());
        assertEquals(List.of(strangerFood.getId(), strangerHome.getId()), categoryIds(breakdown));
        assertEquals(new BigDecimal("70.0"), breakdown.categories().get(0).percentage());
        assertEquals(new BigDecimal("30.0"), breakdown.categories().get(1).percentage());
    }

    @Test
    void breakdown_SumsSeveralTransactionsOfTheSameCategoryIntoOneRow() {
        CategoryBreakdownResponseDto breakdown = breakdownFor(owner, REQUESTED_MONTH);

        assertEquals(4, breakdown.categories().size());
        assertEquals(new BigDecimal("90000.00"), breakdown.categories().get(1).total());
        assertEquals(ownerFood.getId(), breakdown.categories().get(1).categoryId());
    }

    @Test
    void breakdown_OrdersByTotalDescending() {
        CategoryBreakdownResponseDto breakdown = breakdownFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("150000.00"), breakdown.categories().get(0).total());
        assertEquals(new BigDecimal("90000.00"), breakdown.categories().get(1).total());
        assertEquals(new BigDecimal("30000.00"), breakdown.categories().get(2).total());
        assertEquals(new BigDecimal("30000.00"), breakdown.categories().get(3).total());
    }

    @Test
    void breakdown_BreaksEqualTotalsByAscendingCategoryId() {
        CategoryBreakdownResponseDto breakdown = breakdownFor(owner, REQUESTED_MONTH);

        CategoryBreakdownItemDto first = breakdown.categories().get(2);
        CategoryBreakdownItemDto second = breakdown.categories().get(3);

        assertEquals(first.total(), second.total());
        assertTrue(first.categoryId() < second.categoryId());
        assertEquals(Set.of(ownerTravel.getId(), ownerLeisure.getId()),
                Set.of(first.categoryId(), second.categoryId()));
    }

    @Test
    void breakdown_RepeatedCallsReturnTheSameOrder_SoTheDonutColoursDoNotJump() {
        List<Long> first = categoryIds(breakdownFor(owner, REQUESTED_MONTH));
        List<Long> second = categoryIds(breakdownFor(owner, REQUESTED_MONTH));
        List<Long> third = categoryIds(breakdownFor(owner, REQUESTED_MONTH));

        assertEquals(first, second);
        assertEquals(second, third);
    }

    @Test
    void breakdown_ComputesEachShareAgainstTheMonthTotal() {
        CategoryBreakdownResponseDto breakdown = breakdownFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("50.0"), breakdown.categories().get(0).percentage());
        assertEquals(new BigDecimal("30.0"), breakdown.categories().get(1).percentage());
        assertEquals(new BigDecimal("10.0"), breakdown.categories().get(2).percentage());
        assertEquals(new BigDecimal("10.0"), breakdown.categories().get(3).percentage());
    }

    @Test
    void breakdown_ExcludesIncomeEvenWhenItSharesTheCategory() {
        CategoryBreakdownResponseDto breakdown = breakdownFor(owner, REQUESTED_MONTH);

        assertEquals(new BigDecimal("150000.00"), breakdown.categories().get(0).total());
        assertEquals(ownerHome.getId(), breakdown.categories().get(0).categoryId());
        assertEquals(new BigDecimal("300000.00"), breakdown.total());
    }

    @Test
    void breakdown_MonthBoundariesAreInclusiveAndNeighbouringMonthsAreExcluded() {
        CategoryBreakdownResponseDto july = breakdownFor(owner, REQUESTED_MONTH);
        CategoryBreakdownResponseDto june = breakdownFor(owner, "2026-06");
        CategoryBreakdownResponseDto august = breakdownFor(owner, "2026-08");

        assertEquals(new BigDecimal("300000.00"), july.total());
        assertEquals(new BigDecimal("100000.00"), june.total());
        assertEquals(new BigDecimal("888888.00"), august.total());
    }

    @Test
    void breakdown_MonthWithoutExpenses_IsZeroTotalAndAnEmptyListRatherThanAFailure() {
        CategoryBreakdownResponseDto breakdown = breakdownFor(owner, GAP_MONTH);

        assertEquals("2026-05", breakdown.month());
        assertEquals(new BigDecimal("0.00"), breakdown.total());
        assertTrue(breakdown.categories().isEmpty());
    }

    @Test
    void breakdown_ForAUserWithoutAnyTransactions_IsZeroTotalAndAnEmptyList() {
        CategoryBreakdownResponseDto breakdown = breakdownFor(newcomer, REQUESTED_MONTH);

        assertEquals(new BigDecimal("0.00"), breakdown.total());
        assertTrue(breakdown.categories().isEmpty());
    }

    @Test
    void trend_ForTheOwner_FillsTheEmptyMiddleMonthWithZerosInsteadOfDroppingIt() {
        TrendResponseDto trend = trendFor(owner, LAST_TREND_MONTH, 6);

        assertEquals(6, trend.points().size());
        assertEquals(List.of("2026-03", "2026-04", "2026-05", "2026-06", "2026-07", "2026-08"), pointMonths(trend));
        assertEquals("2026-05", trend.points().get(2).month());
        assertEquals(new BigDecimal("0.00"), trend.points().get(2).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(2).expense());
    }

    @Test
    void trend_ForTheOwner_CarriesTheRealMonthlyIncomeAndExpense() {
        TrendResponseDto trend = trendFor(owner, LAST_TREND_MONTH, 6);

        assertEquals(new BigDecimal("430000.00"), trend.points().get(0).income());
        assertEquals(new BigDecimal("298000.00"), trend.points().get(0).expense());
        assertEquals(new BigDecimal("440000.00"), trend.points().get(1).income());
        assertEquals(new BigDecimal("300000.00"), trend.points().get(1).expense());
        assertEquals(new BigDecimal("0.00"), trend.points().get(3).income());
        assertEquals(new BigDecimal("100000.00"), trend.points().get(3).expense());
        assertEquals(new BigDecimal("500000.00"), trend.points().get(4).income());
        assertEquals(new BigDecimal("300000.00"), trend.points().get(4).expense());
        assertEquals(new BigDecimal("0.00"), trend.points().get(5).income());
        assertEquals(new BigDecimal("888888.00"), trend.points().get(5).expense());
    }

    @Test
    void trend_TheOwnersEmptyMonthIsExactlyWhereTheOtherUserHasMoney_SoAMissingUserFilterWouldShowUpHere() {
        TrendResponseDto ownerTrend = trendFor(owner, LAST_TREND_MONTH, 6);
        TrendResponseDto strangerTrend = trendFor(stranger, LAST_TREND_MONTH, 6);

        assertEquals("2026-05", ownerTrend.points().get(2).month());
        assertEquals(new BigDecimal("0.00"), ownerTrend.points().get(2).income());
        assertEquals(new BigDecimal("1000000.00"), strangerTrend.points().get(2).income());
    }

    @Test
    void trend_ForTheStranger_IsComputedFromThatUsersOwnRowsOnly() {
        TrendResponseDto trend = trendFor(stranger, LAST_TREND_MONTH, 6);

        assertEquals(6, trend.points().size());
        assertEquals(new BigDecimal("0.00"), trend.points().get(0).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(1).income());
        assertEquals(new BigDecimal("1000000.00"), trend.points().get(2).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(3).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(4).income());
        assertEquals(new BigDecimal("10000.00"), trend.points().get(4).expense());
        assertEquals(new BigDecimal("2000.00"), trend.points().get(5).income());
    }

    @Test
    void trend_PointsAreChronologicallyAscending() {
        List<String> months = pointMonths(trendFor(owner, LAST_TREND_MONTH, 6));

        assertEquals("2026-03", months.get(0));
        assertEquals("2026-08", months.get(months.size() - 1));
        assertEquals(months.stream().sorted().toList(), months);
    }

    @Test
    void trend_SeriesLengthIsAlwaysTheRequestedMonths_WhateverTheDataLooksLike() {
        assertEquals(1, trendFor(owner, LAST_TREND_MONTH, 1).points().size());
        assertEquals(3, trendFor(owner, LAST_TREND_MONTH, 3).points().size());
        assertEquals(24, trendFor(owner, LAST_TREND_MONTH, 24).points().size());
        assertEquals(24, trendFor(newcomer, LAST_TREND_MONTH, 24).points().size());
    }

    @Test
    void trend_WindowEndingOnTheEmptyMonth_StillCarriesTheEarlierMonths() {
        TrendResponseDto trend = trendFor(owner, GAP_MONTH, 3);

        assertEquals(List.of("2026-03", "2026-04", "2026-05"), pointMonths(trend));
        assertEquals(new BigDecimal("430000.00"), trend.points().get(0).income());
        assertEquals(new BigDecimal("440000.00"), trend.points().get(1).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(2).income());
    }

    @Test
    void trend_WindowThatStartsAfterAllTheDataIsAFullLengthAllZeroSeries() {
        TrendResponseDto trend = trendFor(owner, "2027-06", 6);

        assertEquals(6, trend.points().size());
        assertEquals(List.of("2027-01", "2027-02", "2027-03", "2027-04", "2027-05", "2027-06"), pointMonths(trend));
        assertTrue(trend.points().stream()
                .allMatch(point -> point.income().compareTo(BigDecimal.ZERO) == 0
                        && point.expense().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    void trend_ForAUserWithoutAnyTransactions_IsAFullLengthAllZeroSeriesRatherThanAnEmptyList() {
        TrendResponseDto trend = trendFor(newcomer, LAST_TREND_MONTH, 6);

        assertEquals(6, trend.points().size());
        assertEquals(List.of("2026-03", "2026-04", "2026-05", "2026-06", "2026-07", "2026-08"), pointMonths(trend));
        assertEquals(new BigDecimal("0.00"), trend.points().get(0).income());
        assertEquals(new BigDecimal("0.00"), trend.points().get(5).expense());
    }

    @Test
    void trend_CrossesTheYearBoundaryWithoutLosingOrDuplicatingAMonth() {
        TrendResponseDto trend = trendFor(owner, "2026-02", 4);

        assertEquals(List.of("2025-11", "2025-12", "2026-01", "2026-02"), pointMonths(trend));
        assertTrue(trend.points().stream().allMatch(point -> point.income().compareTo(BigDecimal.ZERO) == 0));
    }
}
