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
import hu.financial.exception.budget.BudgetNotFoundException;
import hu.financial.exception.budget.BudgetValidationException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDate;
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
        testBudget = new Budget(1L, 100.0, LocalDate.now(), testUser, testCategory);
    }

    @Test
    void createBudget_ShouldReturnSavedBudget_WhenValid() {
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        Budget result = budgetService.createBudget(testBudget);

        assertEquals(testBudget, result);
        verify(budgetRepository).save(testBudget);
    }

    @Test
    void createBudget_ShouldThrowValidation_WhenAmountNotPositive() {
        Budget invalid = new Budget(2L, 0.0, LocalDate.now(), testUser, testCategory);

        assertThrows(BudgetValidationException.class, () -> budgetService.createBudget(invalid));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void getAllBudgets_ShouldReturnList_WhenBudgetsExist() {
        when(budgetRepository.findAll()).thenReturn(Arrays.asList(testBudget));

        List<Budget> result = budgetService.getAllBudgets();

        assertEquals(Arrays.asList(testBudget), result);
        verify(budgetRepository).findAll();
    }

    @Test
    void getBudgetsByUserId_ShouldReturnList_WhenBudgetsExist() {
        when(budgetRepository.findByUserId(testUser.getId())).thenReturn(Arrays.asList(testBudget));

        List<Budget> result = budgetService.getBudgetsByUserId(testUser.getId());

        assertEquals(Arrays.asList(testBudget), result);
        verify(budgetRepository).findByUserId(testUser.getId());
    }

    @Test
    void getBudgetById_ShouldReturnBudget_WhenExists() {
        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));

        Budget result = budgetService.getBudgetById(testBudget.getId());

        assertEquals(testBudget, result);
        verify(budgetRepository).findById(testBudget.getId());
    }

    @Test
    void getBudgetById_ShouldThrowNotFound_WhenMissing() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> budgetService.getBudgetById(99L));
    }

    @Test
    void updateBudget_ShouldReturnUpdatedBudget_WhenExists() {
        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        Budget result = budgetService.updateBudget(testBudget.getId(), testBudget);

        assertEquals(testBudget, result);
        verify(budgetRepository).findById(testBudget.getId());
        verify(budgetRepository).save(testBudget);
    }

    @Test
    void updateBudget_ShouldThrowNotFound_WhenMissing() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> budgetService.updateBudget(99L, testBudget));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void deleteBudget_ShouldDelete_WhenExists() {
        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        doNothing().when(budgetRepository).delete(testBudget);

        budgetService.deleteBudget(testBudget.getId());

        verify(budgetRepository).delete(testBudget);
    }

    @Test
    void deleteBudget_ShouldThrowNotFound_WhenMissing() {
        when(budgetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> budgetService.deleteBudget(99L));
        verify(budgetRepository, never()).delete(any(Budget.class));
    }
}
