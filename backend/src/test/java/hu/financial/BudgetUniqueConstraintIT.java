package hu.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import hu.financial.model.Budget;
import hu.financial.model.Category;
import hu.financial.model.User;
import hu.financial.repository.BudgetRepository;
import hu.financial.repository.CategoryRepository;
import hu.financial.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BudgetUniqueConstraintIT {

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
    private BudgetRepository budgetRepository;

    private User owner;

    private User stranger;

    private Category ownerFood;

    private Category ownerTravel;

    private Category strangerFood;

    @BeforeEach
    void seedUsersAndCategories() {
        budgetRepository.deleteAll();
        String run = Long.toString(SEEDS.incrementAndGet());

        owner = userRepository.saveAndFlush(
                new User("constraintowner" + run, "encoded-password", "constraintowner" + run + "@example.com"));
        stranger = userRepository.saveAndFlush(
                new User("constraintstranger" + run, "encoded-password", "constraintstranger" + run + "@example.com"));

        ownerFood = categoryRepository.saveAndFlush(new Category("owner food", "food", owner));
        ownerTravel = categoryRepository.saveAndFlush(new Category("owner travel", "trips", owner));
        strangerFood = categoryRepository.saveAndFlush(new Category("stranger food", "food", stranger));
    }

    private Budget budget(User user, Category category, LocalDate month, String amount) {
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(month.withDayOfMonth(1));
        budget.setAmount(new BigDecimal(amount));
        return budgetRepository.saveAndFlush(budget);
    }

    @Test
    void secondBudget_WithIdenticalMonthValue_ThrowsDataIntegrityViolation() {
        budget(owner, ownerFood, LocalDate.of(2026, 7, 1), "100.00");

        assertThrows(DataIntegrityViolationException.class,
                () -> budget(owner, ownerFood, LocalDate.of(2026, 7, 1), "50.00"));
    }

    @Test
    void secondBudget_ForTheSameCalendarMonthWithADifferentLiteralDay_ThrowsDataIntegrityViolation() {
        budget(owner, ownerFood, LocalDate.of(2026, 7, 1), "100.00");

        assertThrows(DataIntegrityViolationException.class,
                () -> budget(owner, ownerFood, LocalDate.of(2026, 7, 20), "50.00"));
    }

    @Test
    void secondBudget_ForADifferentCategory_DoesNotCollide() {
        budget(owner, ownerFood, LocalDate.of(2026, 7, 1), "100.00");

        assertDoesNotThrow(() -> budget(owner, ownerTravel, LocalDate.of(2026, 7, 1), "50.00"));
    }

    @Test
    void secondBudget_ForADifferentMonth_DoesNotCollide() {
        budget(owner, ownerFood, LocalDate.of(2026, 7, 1), "100.00");

        assertDoesNotThrow(() -> budget(owner, ownerFood, LocalDate.of(2026, 8, 1), "50.00"));
    }

    @Test
    void secondBudget_ForADifferentUserOnTheirOwnCategoryOfTheSameMonth_DoesNotCollide() {
        budget(owner, ownerFood, LocalDate.of(2026, 7, 1), "100.00");

        assertDoesNotThrow(() -> budget(stranger, strangerFood, LocalDate.of(2026, 7, 1), "50.00"));
    }
}
