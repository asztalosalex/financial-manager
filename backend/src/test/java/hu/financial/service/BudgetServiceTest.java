package hu.financial.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import hu.financial.repository.BudgetRepository;
import hu.financial.model.Budget;
import hu.financial.model.User;
import hu.financial.model.Category;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import java.time.YearMonth;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private BudgetService budgetService;

    private Budget testBudget;
    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", "test@example.com");
        testUser.setId(1L);
        testCategory = new Category("testcategory", "testdescription", testUser);
        testCategory.setId(1L);
        testBudget = new Budget(1L, 100.0, YearMonth.now(), testUser, testCategory);
    }

    @Test
    void createBudget_ShouldReturnBudget_WhenValidBudget() {
        // Arrange
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        // Act
        Budget result = budgetService.createBudget(testBudget);

        // Assert
        assertNotNull(result);
    }

    @Test
    void getAllBudgets_ShouldReturnListOfBudgets_WhenBudgetsExist() {
        // Arrange
        when(budgetRepository.findAll()).thenReturn(Arrays.asList(testBudget));

        // Act
        List<Budget> result = budgetService.getAllBudgets();

        // Assert
        assertNotNull(result);
    }


    @Test
    void getBudgetById_ShouldReturnBudget_WhenValidBudget() {
        // Arrange
        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));

        // Act
        Budget result = budgetService.getBudgetById(testBudget.getId());

        // Assert
        assertNotNull(result);
    }

    @Test
    void updateBudget_ShouldReturnBudget_WhenValidBudget() {
        // Arrange
        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        // Act
        Budget result = budgetService.updateBudget(testBudget.getId(), testBudget);

        // Assert
        assertNotNull(result);
    }

    @Test
    void deleteBudget_ShouldReturnVoid_WhenValidBudget() {
        // Arrange
        doNothing().when(budgetRepository).deleteById(testBudget.getId());

        // Act
        budgetService.deleteBudget(testBudget.getId());
    }
}
