package hu.financial.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import hu.financial.repository.BudgetRepository;
import hu.financial.model.Budget;
import hu.financial.model.User;
import hu.financial.model.Category;
import hu.financial.dto.budget.BudgetFilter;
import hu.financial.dto.budget.CreateBudgetDto;
import hu.financial.exception.budget.BudgetNotFoundException;
import hu.financial.exception.budget.BudgetValidationException;
import hu.financial.exception.category.CategoryNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserService userService;

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
        testBudget = new Budget(1L, new BigDecimal("100.00"), LocalDate.now(), testUser, testCategory);
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
        Budget invalid = new Budget(2L, BigDecimal.ZERO, LocalDate.now(), testUser, testCategory);

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
    void getBudgetsByUserId_ShouldDelegateSpecificationAndPageableToRepository_SoFilteringHappensInSql() {
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "month"));
        when(budgetRepository.findAll(ArgumentMatchers.<Specification<Budget>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(Arrays.asList(testBudget), pageable, 11));

        Page<Budget> result = budgetService.getBudgetsByUserId(
                testUser.getId(), BudgetFilter.unfiltered(), pageable);

        assertEquals(Arrays.asList(testBudget), result.getContent());
        assertEquals(11, result.getTotalElements());
        verify(budgetRepository).findAll(ArgumentMatchers.<Specification<Budget>>any(), eq(pageable));
        verify(budgetRepository, never()).findAll(any(Pageable.class));
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
    void updateBudget_ShouldReplaceAmountMonthAndCategory_WhenFullReplacementRequested() {
        Category newCategory = new Category("newcategory", "newdescription", testUser);
        newCategory.setId(2L);
        LocalDate newMonth = LocalDate.now().plusMonths(1);
        Budget replacement = new Budget(null, new BigDecimal("250.00"), newMonth, testUser, newCategory);

        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Budget result = budgetService.updateBudget(testBudget.getId(), replacement);

        assertEquals(new BigDecimal("250.00"), result.getAmount());
        assertEquals(newMonth, result.getMonth());
        assertEquals(newCategory, result.getCategory());
    }

    @Test
    void updateBudget_ShouldThrowValidation_WhenAmountNotPositive() {
        Budget replacement = new Budget(null, BigDecimal.ZERO, LocalDate.now(), testUser, testCategory);
        when(budgetRepository.findById(testBudget.getId())).thenReturn(Optional.of(testBudget));

        assertThrows(BudgetValidationException.class,
                () -> budgetService.updateBudget(testBudget.getId(), replacement));
        verify(budgetRepository, never()).save(any(Budget.class));
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

    @Test
    void mapToEntity_ShouldUseOwnedCategory_AndCurrentUser() {
        CreateBudgetDto dto = new CreateBudgetDto(new BigDecimal("100.00"), LocalDate.now(), testCategory.getId());
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(categoryService.getOwnedCategoryById(testCategory.getId(), testUser.getId())).thenReturn(testCategory);

        Budget result = budgetService.mapToEntity(dto);

        assertEquals(testCategory, result.getCategory());
        assertEquals(testUser, result.getUser());
        verify(categoryService).getOwnedCategoryById(testCategory.getId(), testUser.getId());
    }

    @Test
    void mapToEntity_ShouldPropagateNotFound_WhenCategoryOwnedBySomeoneElse() {
        CreateBudgetDto dto = new CreateBudgetDto(new BigDecimal("100.00"), LocalDate.now(), testCategory.getId());
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(categoryService.getOwnedCategoryById(testCategory.getId(), testUser.getId()))
                .thenThrow(new CategoryNotFoundException(testCategory.getId()));

        assertThrows(CategoryNotFoundException.class, () -> budgetService.mapToEntity(dto));
    }

    @Test
    void mapToEntity_ShouldNormalizeAmountToTwoDecimals_WhenAmountHasFewerDecimals() {
        CreateBudgetDto dto = new CreateBudgetDto(new BigDecimal("100"), LocalDate.now(), testCategory.getId());
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(categoryService.getOwnedCategoryById(testCategory.getId(), testUser.getId())).thenReturn(testCategory);

        Budget result = budgetService.mapToEntity(dto);

        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(2, result.getAmount().scale());
    }
}
