package hu.financial.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import hu.financial.service.BudgetService;
import hu.financial.service.UserService;
import hu.financial.model.User;
import hu.financial.model.Category;
import hu.financial.model.Budget;
import hu.financial.dto.budget.BudgetFilter;
import hu.financial.dto.budget.CreateBudgetDto;
import hu.financial.dto.budget.BudgetResponseDto;
import hu.financial.dto.common.PageResponse;
import hu.financial.exception.InvalidRequestParameterException;
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
import java.time.YearMonth;
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
    void getMyBudgets_ShouldReturnPageWrapper_WhenValidUser() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetsByUserId(eq(testUser.getId()), any(BudgetFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(testBudget), PageRequest.of(0, 20), 1));
        when(budgetService.mapToDto(testBudget)).thenReturn(budgetResponseDto);

        ResponseEntity<PageResponse<BudgetResponseDto>> response =
                budgetController.getMyBudgets(0, 20, "month,desc", null, null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().content().size());
        assertEquals(testBudget.getId(), response.getBody().content().get(0).getId());
        assertEquals(0, response.getBody().page());
        assertEquals(20, response.getBody().size());
        assertEquals(1, response.getBody().totalElements());
        assertTrue(response.getBody().first());
        assertTrue(response.getBody().last());
        verify(budgetService).getBudgetsByUserId(eq(testUser.getId()), eq(BudgetFilter.unfiltered()),
                pageable.capture());
        assertEquals(Sort.by(Sort.Order.desc("month"), Sort.Order.desc("id")), pageable.getValue().getSort());
    }

    @Test
    void getMyBudgets_ShouldPassMonthAndCategoryFiltersToTheService() {
        ArgumentCaptor<BudgetFilter> filter = ArgumentCaptor.forClass(BudgetFilter.class);
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(budgetService.getBudgetsByUserId(eq(testUser.getId()), any(BudgetFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        budgetController.getMyBudgets(0, 20, "month,desc", "2026-02", 7L);

        verify(budgetService).getBudgetsByUserId(eq(testUser.getId()), filter.capture(), any(Pageable.class));
        assertEquals(new BudgetFilter(YearMonth.of(2026, 2), 7L), filter.getValue());
    }

    @Test
    void getMyBudgets_ShouldRejectUnknownSortField_WithoutTouchingTheService() {
        assertThrows(InvalidRequestParameterException.class,
                () -> budgetController.getMyBudgets(0, 20, "user", null, null));
        verify(budgetService, never()).getBudgetsByUserId(any(), any(), any());
    }

    @Test
    void getMyBudgets_ShouldRejectMalformedMonth_WithoutTouchingTheService() {
        assertThrows(InvalidRequestParameterException.class,
                () -> budgetController.getMyBudgets(0, 20, "month,desc", "2026-02-01", null));
        verify(budgetService, never()).getBudgetsByUserId(any(), any(), any());
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
