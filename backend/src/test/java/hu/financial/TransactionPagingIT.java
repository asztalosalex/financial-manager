package hu.financial;

import hu.financial.model.Category;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.CategoryRepository;
import hu.financial.dto.transaction.TransactionFilter;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.UserRepository;
import hu.financial.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TransactionPagingIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15"));

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final AtomicLong SEEDS = new AtomicLong();

    private static final LocalDate SHARED_DATE = LocalDate.of(2026, 1, 1);

    private static final Sort CONTRACT_SORT = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"));

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

    private List<Long> ownerTransactionIds;

    @BeforeEach
    void seedTwoUsersWithTransactions() {
        transactionRepository.deleteAll();
        String run = Long.toString(SEEDS.incrementAndGet());

        owner = userRepository.saveAndFlush(
                new User("pagingowner" + run, "encoded-password", "pagingowner" + run + "@example.com"));
        stranger = userRepository.saveAndFlush(
                new User("pagingstranger" + run, "encoded-password", "pagingstranger" + run + "@example.com"));

        Category ownerCategory = categoryRepository.saveAndFlush(
                new Category("owner groceries", "owner food budget", owner));
        Category strangerCategory = categoryRepository.saveAndFlush(
                new Category("stranger groceries", "stranger food budget", stranger));

        ownerTransactionIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ownerTransactionIds.add(persist(owner, ownerCategory, "owner-" + i).getId());
        }
        for (int i = 0; i < 3; i++) {
            persist(stranger, strangerCategory, "stranger-" + i);
        }
    }

    private Transaction persist(User user, Category category, String description) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.EXPENSE);
        transaction.setDescription(description);
        transaction.setCategory(category);
        transaction.setUser(user);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setDate(SHARED_DATE);
        return transactionRepository.saveAndFlush(transaction);
    }

    @Test
    void pagedQuery_ReturnsOnlyTheOwnersRows_BecauseTheFilterLivesInSql() {
        Page<Transaction> page = transactionService.getTransactionsByUserId(
                owner.getId(), TransactionFilter.unfiltered(), PageRequest.of(0, 20, CONTRACT_SORT));

        assertEquals(5, page.getTotalElements());
        assertEquals(5, page.getContent().size());
        assertTrue(page.getContent().stream().allMatch(t -> t.getUser().getId().equals(owner.getId())));
        assertTrue(page.getContent().stream().noneMatch(t -> t.getDescription().startsWith("stranger-")));
    }

    @Test
    void pagedQuery_TotalElements_CountsOnlyTheOwnerNotEveryUser() {
        long everyRow = transactionRepository.count();
        Page<Transaction> page = transactionService.getTransactionsByUserId(
                owner.getId(), TransactionFilter.unfiltered(), PageRequest.of(0, 2, CONTRACT_SORT));

        assertEquals(8, everyRow);
        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
    }

    @Test
    void pagedQuery_FirstPage_NeverLeaksAnotherUsersRowIntoASmallPage() {
        Page<Transaction> page = transactionService.getTransactionsByUserId(
                stranger.getId(), TransactionFilter.unfiltered(), PageRequest.of(0, 20, CONTRACT_SORT));

        assertEquals(3, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(t -> t.getUser().getId().equals(stranger.getId())));
    }

    @Test
    void pagedQuery_WithIdenticalDates_PagesDoNotOverlapOrDropRows() {
        List<Long> seen = new ArrayList<>();
        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            Page<Transaction> page = transactionService.getTransactionsByUserId(
                    owner.getId(), TransactionFilter.unfiltered(), PageRequest.of(pageNumber, 2, CONTRACT_SORT));
            page.getContent().forEach(t -> seen.add(t.getId()));
        }

        assertEquals(5, seen.size());
        assertEquals(5, seen.stream().distinct().count());
        assertTrue(seen.containsAll(ownerTransactionIds));
    }

    @Test
    void pagedQuery_WithIdenticalDates_OrdersByTheIdTieBreakerDescending() {
        Page<Transaction> page = transactionService.getTransactionsByUserId(
                owner.getId(), TransactionFilter.unfiltered(), PageRequest.of(0, 20, CONTRACT_SORT));

        List<Long> ids = page.getContent().stream().map(Transaction::getId).toList();
        List<Long> descending = new ArrayList<>(ownerTransactionIds);
        descending.sort((left, right) -> Long.compare(right, left));

        assertEquals(descending, ids);
    }

    @Test
    void pagedQuery_BoundaryFlags_DescribeTheOwnersSliceNotTheWholeTable() {
        Page<Transaction> first = transactionService.getTransactionsByUserId(
                owner.getId(), TransactionFilter.unfiltered(), PageRequest.of(0, 2, CONTRACT_SORT));
        Page<Transaction> last = transactionService.getTransactionsByUserId(
                owner.getId(), TransactionFilter.unfiltered(), PageRequest.of(2, 2, CONTRACT_SORT));

        assertTrue(first.isFirst());
        assertFalse(first.isLast());
        assertFalse(last.isFirst());
        assertTrue(last.isLast());
        assertEquals(1, last.getContent().size());
    }
}
