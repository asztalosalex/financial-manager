package hu.financial;

import hu.financial.dto.budget.BudgetFilter;
import hu.financial.model.Budget;
import hu.financial.model.Category;
import hu.financial.model.User;
import hu.financial.repository.BudgetRepository;
import hu.financial.repository.CategoryRepository;
import hu.financial.repository.UserRepository;
import hu.financial.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BudgetPagingIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15"));

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final AtomicLong SEEDS = new AtomicLong();

    private static final Sort CONTRACT_SORT = Sort.by(Sort.Order.desc("month"), Sort.Order.desc("id"));

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20, CONTRACT_SORT);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private BudgetService budgetService;

    private User owner;

    private User stranger;

    private Category ownerGroceries;

    private Category ownerTravel;

    private Category strangerGroceries;

    private Budget januaryFirst;

    private Budget januaryMidMonth;

    private Budget februaryGroceries;

    private Budget februaryTravel;

    private Budget march;

    @BeforeEach
    void seedTwoUsersWithOverlappingBudgets() {
        budgetRepository.deleteAll();
        String run = Long.toString(SEEDS.incrementAndGet());

        owner = userRepository.saveAndFlush(
                new User("budgetowner" + run, "encoded-password", "budgetowner" + run + "@example.com"));
        stranger = userRepository.saveAndFlush(
                new User("budgetstranger" + run, "encoded-password", "budgetstranger" + run + "@example.com"));

        ownerGroceries = categoryRepository.saveAndFlush(new Category("owner groceries", "food", owner));
        ownerTravel = categoryRepository.saveAndFlush(new Category("owner travel", "trips", owner));
        strangerGroceries = categoryRepository.saveAndFlush(new Category("stranger groceries", "food", stranger));

        januaryFirst = persist(owner, ownerGroceries, LocalDate.of(2026, 1, 1), "100.00");
        januaryMidMonth = persist(owner, ownerTravel, LocalDate.of(2026, 1, 15), "200.00");
        februaryGroceries = persist(owner, ownerGroceries, LocalDate.of(2026, 2, 1), "300.00");
        februaryTravel = persist(owner, ownerTravel, LocalDate.of(2026, 2, 1), "400.00");
        march = persist(owner, ownerGroceries, LocalDate.of(2026, 3, 1), "500.00");

        persist(stranger, strangerGroceries, LocalDate.of(2026, 1, 1), "600.00");
        persist(stranger, strangerGroceries, LocalDate.of(2026, 2, 1), "700.00");
    }

    private Budget persist(User user, Category category, LocalDate month, String amount) {
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(month);
        budget.setAmount(new BigDecimal(amount));
        return budgetRepository.saveAndFlush(budget);
    }

    private Page<Budget> query(User user, BudgetFilter filter) {
        return budgetService.getBudgetsByUserId(user.getId(), filter, FIRST_PAGE);
    }

    private static List<Long> idsOf(Page<Budget> page) {
        return page.getContent().stream().map(Budget::getId).toList();
    }

    @Test
    void unfiltered_ReturnsOnlyTheOwnersBudgets_BecauseTheUserFilterLivesInSql() {
        Page<Budget> page = query(owner, BudgetFilter.unfiltered());

        assertEquals(7, budgetRepository.count());
        assertEquals(5, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(b -> b.getUser().getId().equals(owner.getId())));
    }

    @Test
    void unfiltered_ForTheOtherUser_ReturnsOnlyThatUsersBudgets() {
        Page<Budget> page = query(stranger, BudgetFilter.unfiltered());

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(b -> b.getUser().getId().equals(stranger.getId())));
    }

    @Test
    void unfiltered_OrdersByMonthDescendingWithTheIdTieBreaker() {
        Page<Budget> page = query(owner, BudgetFilter.unfiltered());

        assertEquals(List.of(march.getId(), februaryTravel.getId(), februaryGroceries.getId(),
                januaryMidMonth.getId(), januaryFirst.getId()), idsOf(page));
    }

    @Test
    void monthFilter_MatchesTheCalendarMonth_RegardlessOfTheDayComponent() {
        Page<Budget> page = query(owner, new BudgetFilter(YearMonth.of(2026, 1), null));

        assertEquals(2, page.getTotalElements());
        assertEquals(List.of(januaryMidMonth.getId(), januaryFirst.getId()), idsOf(page));
    }

    @Test
    void monthFilter_DoesNotBleedIntoTheNeighbouringMonths() {
        Page<Budget> page = query(owner, new BudgetFilter(YearMonth.of(2026, 2), null));

        assertEquals(2, page.getTotalElements());
        assertEquals(List.of(februaryTravel.getId(), februaryGroceries.getId()), idsOf(page));
    }

    @Test
    void monthFilter_WithoutMatches_ReturnsAnEmptyPage() {
        Page<Budget> page = query(owner, new BudgetFilter(YearMonth.of(2025, 12), null));

        assertEquals(0, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void categoryFilter_MatchesExactlyOneOwnCategory() {
        Page<Budget> page = query(owner, new BudgetFilter(null, ownerGroceries.getId()));

        assertEquals(3, page.getTotalElements());
        assertEquals(List.of(march.getId(), februaryGroceries.getId(), januaryFirst.getId()), idsOf(page));
    }

    @Test
    void categoryFilter_OnAnotherUsersCategory_ReturnsAnEmptyPageInsteadOfLeakingOrFailing() {
        Page<Budget> page = query(owner, new BudgetFilter(null, strangerGroceries.getId()));

        assertEquals(0, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void monthAndCategoryCombined_NarrowToTheSingleMatchingBudget() {
        Page<Budget> page = query(owner, new BudgetFilter(YearMonth.of(2026, 2), ownerTravel.getId()));

        assertEquals(1, page.getTotalElements());
        assertEquals(List.of(februaryTravel.getId()), idsOf(page));
    }

    @Test
    void totalElements_CountsTheFilteredSetNotThePageAndNotTheWholeTable() {
        Page<Budget> page = budgetService.getBudgetsByUserId(owner.getId(),
                new BudgetFilter(YearMonth.of(2026, 1), null), PageRequest.of(0, 1, CONTRACT_SORT));

        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void paging_DoesNotOverlapOrDropRows() {
        List<Long> seen = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            Page<Budget> page = budgetService.getBudgetsByUserId(owner.getId(), BudgetFilter.unfiltered(),
                    PageRequest.of(pageNumber, 2, CONTRACT_SORT));
            page.getContent().forEach(budget -> seen.add(budget.getId()));
        }

        assertEquals(5, seen.size());
        assertEquals(5, seen.stream().distinct().count());
    }

    @Test
    void boundaryFlags_DescribeTheOwnersSliceNotTheWholeTable() {
        Page<Budget> first = budgetService.getBudgetsByUserId(owner.getId(), BudgetFilter.unfiltered(),
                PageRequest.of(0, 2, CONTRACT_SORT));
        Page<Budget> last = budgetService.getBudgetsByUserId(owner.getId(), BudgetFilter.unfiltered(),
                PageRequest.of(2, 2, CONTRACT_SORT));

        assertTrue(first.isFirst());
        assertFalse(first.isLast());
        assertFalse(last.isFirst());
        assertTrue(last.isLast());
        assertEquals(1, last.getContent().size());
        assertEquals(3, first.getTotalPages());
    }

    @Test
    void sortByAmount_UsesTheRequestedKeyWithTheIdTieBreaker() {
        Page<Budget> page = budgetService.getBudgetsByUserId(owner.getId(), BudgetFilter.unfiltered(),
                PageRequest.of(0, 20, Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("id"))));

        assertEquals(List.of(januaryFirst.getId(), januaryMidMonth.getId(), februaryGroceries.getId(),
                februaryTravel.getId(), march.getId()), idsOf(page));
    }
}
