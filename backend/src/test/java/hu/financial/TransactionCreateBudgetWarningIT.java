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

import hu.financial.dto.transaction.TransactionResponseDto;
import hu.financial.model.Budget;
import hu.financial.model.Category;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.BudgetRepository;
import hu.financial.repository.CategoryRepository;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.UserRepository;
import hu.financial.service.TransactionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TransactionCreateBudgetWarningIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15"));

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final AtomicLong SEEDS = new AtomicLong();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionService transactionService;

    private User owner;

    private Category groceries;

    @BeforeEach
    void seedOwnerWithABudgetedCategory() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        String run = Long.toString(SEEDS.incrementAndGet());

        owner = userRepository.saveAndFlush(
                new User("warningowner" + run, "encoded-password", "warningowner" + run + "@example.com"));
        groceries = categoryRepository.saveAndFlush(new Category("groceries" + run, "food", owner));

        Budget budget = new Budget();
        budget.setUser(owner);
        budget.setCategory(groceries);
        budget.setMonth(LocalDate.of(2026, 7, 1));
        budget.setAmount(new BigDecimal("150000.00"));
        budgetRepository.saveAndFlush(budget);

        priorExpense(new BigDecimal("140000.00"), LocalDate.of(2026, 7, 10));
    }

    private void priorExpense(BigDecimal amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.EXPENSE);
        transaction.setDescription("prior spending");
        transaction.setCategory(groceries);
        transaction.setUser(owner);
        transaction.setAmount(amount);
        transaction.setDate(date);
        transactionRepository.saveAndFlush(transaction);
    }

    private Transaction newExpense(BigDecimal amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.EXPENSE);
        transaction.setDescription("the new transaction under test");
        transaction.setCategory(groceries);
        transaction.setUser(owner);
        transaction.setAmount(amount);
        transaction.setDate(date);
        return transaction;
    }

    @Test
    void aTransactionThatCrossesTheThresholdOnlyBecauseOfItself_StillGetsABudgetWarning() {
        Transaction saved = transactionService.createTransaction(newExpense(new BigDecimal("15000.00"), LocalDate.of(2026, 7, 20)));

        TransactionResponseDto response = transactionService.mapToDtoWithBudgetWarning(saved);

        assertNotNull(response.budgetWarning());
        assertEquals(new BigDecimal("150000.00"), response.budgetWarning().budgeted());
        assertEquals(new BigDecimal("155000.00"), response.budgetWarning().spent());
        assertEquals(new BigDecimal("-5000.00"), response.budgetWarning().remaining());
    }

    @Test
    void aTransactionThatStaysUnderTheThreshold_EvenIncludingItself_GetsNoWarning() {
        Transaction saved = transactionService.createTransaction(newExpense(new BigDecimal("5000.00"), LocalDate.of(2026, 7, 20)));

        TransactionResponseDto response = transactionService.mapToDtoWithBudgetWarning(saved);

        assertNull(response.budgetWarning());
    }

    @Test
    void aTransactionInACategoryWithNoBudgetForItsMonth_GetsNoWarning_RatherThanFailing() {
        Category unbudgeted = categoryRepository.saveAndFlush(new Category("unbudgeted", "no budget set", owner));
        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.EXPENSE);
        transaction.setDescription("no budget category");
        transaction.setCategory(unbudgeted);
        transaction.setUser(owner);
        transaction.setAmount(new BigDecimal("999999.00"));
        transaction.setDate(LocalDate.of(2026, 7, 20));
        Transaction saved = transactionService.createTransaction(transaction);

        TransactionResponseDto response = transactionService.mapToDtoWithBudgetWarning(saved);

        assertNull(response.budgetWarning());
    }

    @Test
    void aPastDatedTransaction_IsJudgedAgainstItsOwnMonthsBudget_NotTheRealCurrentMonth() {
        Category pastCategory = categoryRepository.saveAndFlush(new Category("pastcategory", "old month", owner));
        Budget pastBudget = new Budget();
        pastBudget.setUser(owner);
        pastBudget.setCategory(pastCategory);
        pastBudget.setMonth(LocalDate.of(2019, 3, 1));
        pastBudget.setAmount(new BigDecimal("100.00"));
        budgetRepository.saveAndFlush(pastBudget);

        Transaction transaction = new Transaction();
        transaction.setType(TransactionType.EXPENSE);
        transaction.setDescription("long ago overspend");
        transaction.setCategory(pastCategory);
        transaction.setUser(owner);
        transaction.setAmount(new BigDecimal("150.00"));
        transaction.setDate(LocalDate.of(2019, 3, 15));
        Transaction saved = transactionService.createTransaction(transaction);

        TransactionResponseDto response = transactionService.mapToDtoWithBudgetWarning(saved);

        assertNotNull(response.budgetWarning());
        assertEquals(new BigDecimal("-50.00"), response.budgetWarning().remaining());
    }
}
