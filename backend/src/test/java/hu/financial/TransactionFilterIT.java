package hu.financial;

import hu.financial.dto.transaction.TransactionFilter;
import hu.financial.model.Category;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.CategoryRepository;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.UserRepository;
import hu.financial.service.TransactionService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TransactionFilterIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15"));

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final AtomicLong SEEDS = new AtomicLong();

    private static final LocalDate EARLY = LocalDate.of(2026, 1, 5);

    private static final LocalDate MIDDLE = LocalDate.of(2026, 2, 10);

    private static final LocalDate LATE = LocalDate.of(2026, 3, 1);

    private static final Sort CONTRACT_SORT = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"));

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20, CONTRACT_SORT);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    private User owner;

    private User stranger;

    private Category ownerGroceries;

    private Category ownerSalary;

    private Category strangerGroceries;

    private Transaction earlyExpense;

    private Transaction earlyIncome;

    private Transaction middleExpense;

    private Transaction lateIncome;

    @BeforeEach
    void seedTwoUsersWithOverlappingData() {
        transactionRepository.deleteAll();
        String run = Long.toString(SEEDS.incrementAndGet());

        owner = userRepository.saveAndFlush(
                new User("filterowner" + run, "encoded-password", "filterowner" + run + "@example.com"));
        stranger = userRepository.saveAndFlush(
                new User("filterstranger" + run, "encoded-password", "filterstranger" + run + "@example.com"));

        ownerGroceries = categoryRepository.saveAndFlush(new Category("owner groceries", "food", owner));
        ownerSalary = categoryRepository.saveAndFlush(new Category("owner salary", "income", owner));
        strangerGroceries = categoryRepository.saveAndFlush(new Category("stranger groceries", "food", stranger));

        earlyExpense = persist(owner, ownerGroceries, TransactionType.EXPENSE, EARLY, "owner-early-expense");
        earlyIncome = persist(owner, ownerGroceries, TransactionType.INCOME, EARLY, "owner-early-income");
        middleExpense = persist(owner, ownerSalary, TransactionType.EXPENSE, MIDDLE, "owner-middle-expense");
        lateIncome = persist(owner, ownerSalary, TransactionType.INCOME, LATE, "owner-late-income");

        persist(stranger, strangerGroceries, TransactionType.EXPENSE, EARLY, "stranger-early-expense");
        persist(stranger, strangerGroceries, TransactionType.INCOME, MIDDLE, "stranger-middle-income");
        persist(stranger, strangerGroceries, TransactionType.INCOME, LATE, "stranger-late-income");
    }

    private Transaction persist(User user, Category category, TransactionType type, LocalDate date, String description) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setCategory(category);
        transaction.setUser(user);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setDate(date);
        return transactionRepository.saveAndFlush(transaction);
    }

    private Page<Transaction> query(User user, TransactionFilter filter) {
        return transactionService.getTransactionsByUserId(user.getId(), filter, FIRST_PAGE);
    }

    private static List<Long> idsOf(Page<Transaction> page) {
        return page.getContent().stream().map(Transaction::getId).toList();
    }

    @Test
    void unfiltered_ReturnsOnlyTheOwnersRows_BecauseTheUserFilterLivesInSql() {
        Page<Transaction> page = query(owner, TransactionFilter.unfiltered());

        assertEquals(7, transactionRepository.count());
        assertEquals(4, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(t -> t.getUser().getId().equals(owner.getId())));
        assertTrue(page.getContent().stream().noneMatch(t -> t.getDescription().startsWith("stranger-")));
    }

    @Test
    void unfiltered_ForTheOtherUser_ReturnsOnlyThatUsersRows() {
        Page<Transaction> page = query(stranger, TransactionFilter.unfiltered());

        assertEquals(3, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(t -> t.getUser().getId().equals(stranger.getId())));
    }

    @Test
    void unfiltered_OrdersByDateDescendingWithTheIdTieBreaker() {
        Page<Transaction> page = query(owner, TransactionFilter.unfiltered());

        assertEquals(List.of(lateIncome.getId(), middleExpense.getId(), earlyIncome.getId(), earlyExpense.getId()),
                idsOf(page));
    }

    @Test
    void fromFilter_IsInclusiveOnItsOwnBoundary() {
        Page<Transaction> onBoundary = query(owner, new TransactionFilter(EARLY, null, null, null));
        Page<Transaction> afterBoundary = query(owner, new TransactionFilter(MIDDLE, null, null, null));

        assertEquals(4, onBoundary.getTotalElements());
        assertEquals(2, afterBoundary.getTotalElements());
        assertEquals(List.of(lateIncome.getId(), middleExpense.getId()), idsOf(afterBoundary));
    }

    @Test
    void toFilter_IsInclusiveOnItsOwnBoundary() {
        Page<Transaction> page = query(owner, new TransactionFilter(null, EARLY, null, null));

        assertEquals(2, page.getTotalElements());
        assertEquals(List.of(earlyIncome.getId(), earlyExpense.getId()), idsOf(page));
    }

    @Test
    void fromAndTo_NarrowToASingleDay_WithoutLeakingTheOtherUser() {
        Page<Transaction> page = query(owner, new TransactionFilter(MIDDLE, MIDDLE, null, null));

        assertEquals(1, page.getTotalElements());
        assertEquals(List.of(middleExpense.getId()), idsOf(page));
    }

    @Test
    void categoryFilter_MatchesExactlyOneOwnCategory() {
        Page<Transaction> page = query(owner, new TransactionFilter(null, null, ownerGroceries.getId(), null));

        assertEquals(2, page.getTotalElements());
        assertEquals(List.of(earlyIncome.getId(), earlyExpense.getId()), idsOf(page));
    }

    @Test
    void categoryFilter_OnAnotherUsersCategory_ReturnsAnEmptyPageInsteadOfLeakingOrFailing() {
        Page<Transaction> page = query(owner, new TransactionFilter(null, null, strangerGroceries.getId(), null));

        assertEquals(0, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
        assertTrue(page.isFirst());
    }

    @Test
    void categoryFilter_OnAnUnknownCategory_ReturnsAnEmptyPage() {
        Page<Transaction> page = query(owner, new TransactionFilter(null, null, -1L, null));

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void typeFilter_MatchesExactly() {
        Page<Transaction> income = query(owner, new TransactionFilter(null, null, null, TransactionType.INCOME));
        Page<Transaction> expense = query(owner, new TransactionFilter(null, null, null, TransactionType.EXPENSE));

        assertEquals(List.of(lateIncome.getId(), earlyIncome.getId()), idsOf(income));
        assertEquals(List.of(middleExpense.getId(), earlyExpense.getId()), idsOf(expense));
    }

    @Test
    void everyFilterCombined_NarrowsToTheSingleMatchingRow() {
        Page<Transaction> page = query(owner, new TransactionFilter(EARLY, MIDDLE, ownerGroceries.getId(),
                TransactionType.INCOME));

        assertEquals(1, page.getTotalElements());
        assertEquals(List.of(earlyIncome.getId()), idsOf(page));
    }

    @Test
    void totalElements_CountsTheFilteredSetNotThePageAndNotTheWholeTable() {
        Pageable singleRowPage = PageRequest.of(0, 1, CONTRACT_SORT);

        Page<Transaction> page = transactionService.getTransactionsByUserId(owner.getId(),
                new TransactionFilter(null, null, null, TransactionType.INCOME), singleRowPage);

        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void filteredPaging_DoesNotOverlapOrDropRows() {
        TransactionFilter filter = new TransactionFilter(EARLY, LATE, null, null);
        List<Long> seen = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < 2; pageNumber++) {
            Page<Transaction> page = transactionService.getTransactionsByUserId(owner.getId(), filter,
                    PageRequest.of(pageNumber, 3, CONTRACT_SORT));
            page.getContent().forEach(transaction -> seen.add(transaction.getId()));
        }

        assertEquals(4, seen.size());
        assertEquals(4, seen.stream().distinct().count());
        assertTrue(seen.containsAll(List.of(earlyExpense.getId(), earlyIncome.getId(), middleExpense.getId(),
                lateIncome.getId())));
    }
}
