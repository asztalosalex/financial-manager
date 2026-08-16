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

import hu.financial.dto.common.PageResponse;
import hu.financial.dto.transaction.TransactionFilter;
import hu.financial.exception.InvalidRequestParameterException;
import hu.financial.service.TransactionService;
import hu.financial.service.UserService;
import hu.financial.model.User;
import hu.financial.model.Category;
import hu.financial.model.Transaction;
import hu.financial.model.enums.TransactionType;
import hu.financial.dto.transaction.CreateTransactionDto;
import hu.financial.dto.transaction.TransactionResponseDto;
import hu.financial.exception.category.CategoryNotFoundException;
import hu.financial.exception.transaction.TransactionNotFoundException;

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
public class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionController transactionController;

    private User testUser;
    private Category testCategory;
    private Transaction testTransaction;
    private CreateTransactionDto createTransactionDto;
    private TransactionResponseDto transactionResponseDto;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", "test@example.com");
        testUser.setId(1L);

        testCategory = new Category("testcategory", "testdescription", testUser);
        testCategory.setId(1L);

        testTransaction = new Transaction(1L, TransactionType.INCOME, "testdescription",
                testCategory, testUser, new BigDecimal("100.00"), LocalDate.now());

        createTransactionDto = new CreateTransactionDto(TransactionType.INCOME, "testdescription",
                testCategory.getId(), new BigDecimal("100.00"), LocalDate.now());

        transactionResponseDto = new TransactionResponseDto(testTransaction.getId(), TransactionType.INCOME,
                "testdescription", testCategory.getId(), testCategory.getName(), new BigDecimal("100.00"), testTransaction.getDate());
    }

    @Test
    void createTransaction_ShouldReturnCreated_WhenValidTransaction() {
        when(transactionService.mapToEntity(any(CreateTransactionDto.class))).thenReturn(testTransaction);
        when(transactionService.createTransaction(any(Transaction.class))).thenReturn(testTransaction);
        when(transactionService.mapToDto(any(Transaction.class))).thenReturn(transactionResponseDto);

        ResponseEntity<TransactionResponseDto> response = transactionController.createTransaction(createTransactionDto);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testTransaction.getId(), response.getBody().id());
        verify(transactionService).createTransaction(any(Transaction.class));
    }

    @Test
    void getMyTransactions_ShouldReturnPageWrapper_WhenValidUser() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionsByUserId(eq(testUser.getId()), any(TransactionFilter.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(testTransaction), PageRequest.of(0, 20), 1));
        when(transactionService.mapToDto(testTransaction)).thenReturn(transactionResponseDto);

        ResponseEntity<PageResponse<TransactionResponseDto>> response =
                transactionController.getMyTransactions(0, 20, "date,desc", null, null, null, null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().content().size());
        assertEquals(testTransaction.getId(), response.getBody().content().get(0).id());
        assertEquals(0, response.getBody().page());
        assertEquals(20, response.getBody().size());
        assertEquals(1, response.getBody().totalElements());
        assertTrue(response.getBody().first());
        assertTrue(response.getBody().last());
        verify(transactionService).getTransactionsByUserId(eq(testUser.getId()), eq(TransactionFilter.unfiltered()),
                pageable.capture());
        assertEquals(Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id")), pageable.getValue().getSort());
    }

    @Test
    void getMyTransactions_ShouldPassEveryFilterToTheService() {
        ArgumentCaptor<TransactionFilter> filter = ArgumentCaptor.forClass(TransactionFilter.class);
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionsByUserId(eq(testUser.getId()), any(TransactionFilter.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        transactionController.getMyTransactions(0, 20, "date,desc", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), 7L, TransactionType.EXPENSE);

        verify(transactionService).getTransactionsByUserId(eq(testUser.getId()), filter.capture(),
                any(Pageable.class));
        assertEquals(new TransactionFilter(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 7L,
                TransactionType.EXPENSE), filter.getValue());
    }

    @Test
    void getMyTransactions_ShouldRejectUnknownSortField_WithoutTouchingTheService() {
        assertThrows(InvalidRequestParameterException.class,
                () -> transactionController.getMyTransactions(0, 20, "password", null, null, null, null));
        verify(transactionService, never()).getTransactionsByUserId(any(), any(), any());
    }

    @Test
    void getMyTransactions_ShouldRejectInvertedDateRange_WithoutTouchingTheService() {
        assertThrows(InvalidRequestParameterException.class,
                () -> transactionController.getMyTransactions(0, 20, "date,desc", LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 31), null, null));
        verify(transactionService, never()).getTransactionsByUserId(any(), any(), any());
    }

    @Test
    void getTransactionById_ShouldReturnTransaction_WhenOwnedByCurrentUser() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionById(testTransaction.getId())).thenReturn(testTransaction);
        when(transactionService.mapToDto(testTransaction)).thenReturn(transactionResponseDto);

        ResponseEntity<TransactionResponseDto> response = transactionController.getTransactionById(testTransaction.getId());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testTransaction.getId(), response.getBody().id());
    }

    @Test
    void getTransactionById_ShouldThrowNotFound_WhenTransactionMissing() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionById(99L)).thenReturn(null);

        assertThrows(TransactionNotFoundException.class,
                () -> transactionController.getTransactionById(99L));
    }

    @Test
    void getTransactionById_ShouldThrowNotFound_WhenOwnedByAnotherUser() {
        User otherUser = new User("other", "password123", "other@example.com");
        otherUser.setId(2L);
        Transaction foreignTransaction = new Transaction(5L, TransactionType.EXPENSE, "foreign",
                testCategory, otherUser, new BigDecimal("50.00"), LocalDate.now());

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionById(foreignTransaction.getId())).thenReturn(foreignTransaction);

        assertThrows(TransactionNotFoundException.class,
                () -> transactionController.getTransactionById(foreignTransaction.getId()));
    }

    @Test
    void updateTransaction_ShouldReturnTransaction_WhenOwnedByCurrentUser() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionById(testTransaction.getId())).thenReturn(testTransaction);
        when(transactionService.mapToEntity(any(CreateTransactionDto.class))).thenReturn(testTransaction);
        when(transactionService.updateTransaction(eq(testTransaction.getId()), any(Transaction.class))).thenReturn(testTransaction);
        when(transactionService.mapToDto(testTransaction)).thenReturn(transactionResponseDto);

        ResponseEntity<TransactionResponseDto> response = transactionController.updateTransaction(testTransaction.getId(), createTransactionDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(transactionService).updateTransaction(eq(testTransaction.getId()), any(Transaction.class));
    }

    @Test
    void updateTransaction_ShouldThrowNotFound_WhenNewCategoryOwnedByAnotherUser() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionById(testTransaction.getId())).thenReturn(testTransaction);
        when(transactionService.mapToEntity(any(CreateTransactionDto.class)))
                .thenThrow(new CategoryNotFoundException(99L));

        assertThrows(CategoryNotFoundException.class,
                () -> transactionController.updateTransaction(testTransaction.getId(), createTransactionDto));
        verify(transactionService, never()).updateTransaction(any(), any());
    }

    @Test
    void updateTransaction_ShouldThrowNotFound_WhenOwnedByAnotherUser() {
        User otherUser = new User("other", "password123", "other@example.com");
        otherUser.setId(2L);
        Transaction foreignTransaction = new Transaction(5L, TransactionType.EXPENSE, "foreign",
                testCategory, otherUser, new BigDecimal("50.00"), LocalDate.now());

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionById(foreignTransaction.getId())).thenReturn(foreignTransaction);

        assertThrows(TransactionNotFoundException.class,
                () -> transactionController.updateTransaction(foreignTransaction.getId(), createTransactionDto));
        verify(transactionService, never()).updateTransaction(any(), any());
    }

    @Test
    void deleteTransaction_ShouldReturnNoContent_WhenOwnedByCurrentUser() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionById(testTransaction.getId())).thenReturn(testTransaction);
        doNothing().when(transactionService).deleteTransaction(testTransaction.getId());

        ResponseEntity<Void> response = transactionController.deleteTransaction(testTransaction.getId());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(transactionService).deleteTransaction(testTransaction.getId());
    }

    @Test
    void deleteTransaction_ShouldThrowNotFound_WhenOwnedByAnotherUser() {
        User otherUser = new User("other", "password123", "other@example.com");
        otherUser.setId(2L);
        Transaction foreignTransaction = new Transaction(5L, TransactionType.EXPENSE, "foreign",
                testCategory, otherUser, new BigDecimal("50.00"), LocalDate.now());

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(transactionService.getTransactionById(foreignTransaction.getId())).thenReturn(foreignTransaction);

        assertThrows(TransactionNotFoundException.class,
                () -> transactionController.deleteTransaction(foreignTransaction.getId()));
        verify(transactionService, never()).deleteTransaction(any());
    }
}
