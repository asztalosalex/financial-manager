package hu.financial.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hu.financial.service.BudgetService;
import hu.financial.service.UserService;
import hu.financial.model.User;
import hu.financial.model.Category;
import hu.financial.model.Budget;
import hu.financial.dto.budget.CreateBudgetDto;
import hu.financial.dto.budget.BudgetResponseDto;
import hu.financial.exception.budget.BudgetNotFoundException;
import hu.financial.exception.category.CategoryNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class BudgetControllerTest {

    @Mock
    private BudgetService budgetService;

    @Mock
    private UserService userService;

    @InjectMocks
    private BudgetController budgetController;

    private User testUser;
    private Category testCategory;
    private Budget testBudget;
    private CreateBudgetDto createBudgetDto;
    private BudgetResponseDto budgetResponseDto;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", "test@example.com");
        testUser.setId(1L);

        testCategory = new Category("testcategory", "testdescription", testUser);
        testCategory.setId(1L);

        testBudget = new Budget(1L, new BigDecimal("100.00"), LocalDate.now(), testUser, testCategory);

        createBudgetDto = new CreateBudgetDto(new BigDecimal("100.00"), LocalDate.now(), testCategory.getId());

        budgetResponseDto = new BudgetResponseDto(testBudget.getId(), new BigDecimal("100.00"), testBudget.getMonth(),
                testCategory.getId(), testCategory.getName());
    }

    @Test
    void createBudget_ShouldReturnCreated_WhenValidBudget() {
        when(budgetService.mapToEntity(any(CreateBudgetDto.class))).thenReturn(testBudget);
        when(budgetService.createBudget(any(Budget.class))).thenReturn(testBudget);
        when(budgetService.mapToDto(any(Budget.class))).thenReturn(budgetResponseDto);

        ResponseEntity<BudgetResponseDto> response = budgetController.createBudget(createBudgetDto);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testBudget.getId(), response.getBody().getId());
        verify(budgetService).createBudget(any(Budget.class));
    }

    @Test
    void getMyBudgets_ShouldReturnList_WhenValidUser() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetsByUserId(testUser.getId())).thenReturn(Arrays.asList(testBudget));
        when(budgetService.mapToDto(testBudget)).thenReturn(budgetResponseDto);

        ResponseEntity<List<BudgetResponseDto>> response = budgetController.getMyBudgets();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testBudget.getId(), response.getBody().get(0).getId());
        verify(budgetService).getBudgetsByUserId(testUser.getId());
    }

    @Test
    void getBudgetById_ShouldReturnBudget_WhenOwnedByCurrentUser() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetById(testBudget.getId())).thenReturn(testBudget);
        when(budgetService.mapToDto(testBudget)).thenReturn(budgetResponseDto);

        ResponseEntity<BudgetResponseDto> response = budgetController.getBudgetById(testBudget.getId());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testBudget.getId(), response.getBody().getId());
    }

    @Test
    void getBudgetById_ShouldThrowNotFound_WhenBudgetMissing() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetById(99L)).thenReturn(null);

        assertThrows(BudgetNotFoundException.class,
                () -> budgetController.getBudgetById(99L));
    }

    @Test
    void getBudgetById_ShouldThrowNotFound_WhenOwnedByAnotherUser() {
        User otherUser = new User("other", "password123", "other@example.com");
        otherUser.setId(2L);
        Budget foreignBudget = new Budget(5L, new BigDecimal("50.00"), LocalDate.now(), otherUser, testCategory);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetById(foreignBudget.getId())).thenReturn(foreignBudget);

        assertThrows(BudgetNotFoundException.class,
                () -> budgetController.getBudgetById(foreignBudget.getId()));
    }

    @Test
    void updateBudget_ShouldReturnBudget_WhenOwnedByCurrentUser() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetById(testBudget.getId())).thenReturn(testBudget);
        when(budgetService.mapToEntity(any(CreateBudgetDto.class))).thenReturn(testBudget);
        when(budgetService.updateBudget(eq(testBudget.getId()), any(Budget.class))).thenReturn(testBudget);
        when(budgetService.mapToDto(testBudget)).thenReturn(budgetResponseDto);

        ResponseEntity<BudgetResponseDto> response = budgetController.updateBudget(testBudget.getId(), createBudgetDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(budgetService).updateBudget(eq(testBudget.getId()), any(Budget.class));
    }

    @Test
    void updateBudget_ShouldThrowNotFound_WhenNewCategoryOwnedByAnotherUser() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetById(testBudget.getId())).thenReturn(testBudget);
        when(budgetService.mapToEntity(any(CreateBudgetDto.class)))
                .thenThrow(new CategoryNotFoundException(99L));

        assertThrows(CategoryNotFoundException.class,
                () -> budgetController.updateBudget(testBudget.getId(), createBudgetDto));
        verify(budgetService, never()).updateBudget(any(), any());
    }

    @Test
    void updateBudget_ShouldThrowNotFound_WhenOwnedByAnotherUser() {
        User otherUser = new User("other", "password123", "other@example.com");
        otherUser.setId(2L);
        Budget foreignBudget = new Budget(5L, new BigDecimal("50.00"), LocalDate.now(), otherUser, testCategory);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetById(foreignBudget.getId())).thenReturn(foreignBudget);

        assertThrows(BudgetNotFoundException.class,
                () -> budgetController.updateBudget(foreignBudget.getId(), createBudgetDto));
        verify(budgetService, never()).updateBudget(any(), any());
    }

    @Test
    void deleteBudget_ShouldReturnNoContent_WhenOwnedByCurrentUser() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetById(testBudget.getId())).thenReturn(testBudget);
        doNothing().when(budgetService).deleteBudget(testBudget.getId());

        ResponseEntity<Void> response = budgetController.deleteBudget(testBudget.getId());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(budgetService).deleteBudget(testBudget.getId());
    }

    @Test
    void deleteBudget_ShouldThrowNotFound_WhenOwnedByAnotherUser() {
        User otherUser = new User("other", "password123", "other@example.com");
        otherUser.setId(2L);
        Budget foreignBudget = new Budget(5L, new BigDecimal("50.00"), LocalDate.now(), otherUser, testCategory);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetById(foreignBudget.getId())).thenReturn(foreignBudget);

        assertThrows(BudgetNotFoundException.class,
                () -> budgetController.deleteBudget(foreignBudget.getId()));
        verify(budgetService, never()).deleteBudget(any());
    }
}
