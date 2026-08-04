package hu.financial.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import hu.financial.repository.TransactionRepository;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.model.Category;
import hu.financial.dto.transaction.CreateTransactionDto;
import hu.financial.dto.transaction.TransactionFilter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.time.LocalDate;
import hu.financial.model.enums.TransactionType;
import hu.financial.exception.category.CategoryNotFoundException;
import hu.financial.exception.transaction.TransactionNotFoundException;
import hu.financial.exception.transaction.TransactionValidationException;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction testTransaction;
    private User testUser;
    private Category testCategory;
    private TransactionType testTransactionType;
    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", "test@example.com");
        testUser.setId(1L);
        testCategory = new Category("testcategory", "testdescription", testUser);
        testCategory.setId(1L);
        testTransactionType = TransactionType.INCOME;
        testTransaction = new Transaction(1L, testTransactionType, "testdescription", testCategory, testUser, new BigDecimal("100.00"), LocalDate.now());
    }


    @Test
    void createTransaction_ShouldReturnTransaction_WhenValidTransaction() {
        // Arrange
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        Transaction result = transactionService.createTransaction(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals(testTransaction, result);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void getAllTransactions_ShouldReturnListOfTransactions_WhenTransactionsExist() {
        // Arrange
        when(transactionRepository.findAll()).thenReturn(Arrays.asList(testTransaction));

        // Act
        List<Transaction> result = transactionService.getAllTransactions();

        // Assert
        assertNotNull(result);
        assertEquals(Arrays.asList(testTransaction), result);
        verify(transactionRepository, times(1)).findAll();
    }

    @Test
    void getTransactionById_ShouldReturnTransaction_WhenValidTransaction() {
        // Arrange
        when(transactionRepository.findById(testTransaction.getId())).thenReturn(Optional.of(testTransaction));

        // Act
        Transaction result = transactionService.getTransactionById(testTransaction.getId());
    
        // Assert
        assertNotNull(result);
        assertEquals(testTransaction, result);
        verify(transactionRepository, times(1)).findById(testTransaction.getId());
    }

    @Test
    void updateTransaction_ShouldReturnTransaction_WhenValidTransaction() {
        // Arrange
        when(transactionRepository.findById(testTransaction.getId())).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        Transaction result = transactionService.updateTransaction(testTransaction.getId(), testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals(testTransaction, result);
        verify(transactionRepository, times(1)).findById(testTransaction.getId());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void deleteTransaction_ShouldReturnVoid_WhenValidTransaction() {
        // Arrange
        when(transactionRepository.findById(testTransaction.getId())).thenReturn(Optional.of(testTransaction));
        doNothing().when(transactionRepository).delete(testTransaction);

        // Act
        transactionService.deleteTransaction(testTransaction.getId());

        // Assert
        verify(transactionRepository, times(1)).delete(testTransaction);
    }

    @Test
    void getTransactionsByUserId_ShouldDelegateSpecificationAndPageableToRepository_SoFilteringHappensInSql() {
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "date"));
        Page<Transaction> page = new PageImpl<>(Arrays.asList(testTransaction), pageable, 11);
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(pageable)))
                .thenReturn(page);

        Page<Transaction> result = transactionService.getTransactionsByUserId(
                testUser.getId(), TransactionFilter.unfiltered(), pageable);

        assertNotNull(result);
        assertEquals(Arrays.asList(testTransaction), result.getContent());
        assertEquals(11, result.getTotalElements());
        verify(transactionRepository, times(1))
                .findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(pageable));
        verify(transactionRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void createTransaction_ShouldThrowValidation_WhenAmountNotPositive() {
        Transaction invalid = new Transaction(2L, testTransactionType, "invalid",
                testCategory, testUser, BigDecimal.ZERO, LocalDate.now());

        assertThrows(TransactionValidationException.class, () -> transactionService.createTransaction(invalid));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionById_ShouldThrowNotFound_WhenMissing() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransactionById(99L));
    }

    @Test
    void updateTransaction_ShouldReplaceAllMutableFields_WhenFullReplacementRequested() {
        Category newCategory = new Category("newcategory", "newdescription", testUser);
        newCategory.setId(2L);
        LocalDate newDate = LocalDate.now().minusDays(3);
        Transaction replacement = new Transaction(null, TransactionType.EXPENSE, "new description",
                newCategory, testUser, new BigDecimal("42.50"), newDate);

        when(transactionRepository.findById(testTransaction.getId())).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.updateTransaction(testTransaction.getId(), replacement);

        assertEquals(TransactionType.EXPENSE, result.getType());
        assertEquals("new description", result.getDescription());
        assertEquals(newCategory, result.getCategory());
        assertEquals(new BigDecimal("42.50"), result.getAmount());
        assertEquals(newDate, result.getDate());
    }

    @Test
    void updateTransaction_ShouldThrowValidation_WhenAmountNotPositive() {
        Transaction replacement = new Transaction(null, TransactionType.EXPENSE, "invalid",
                testCategory, testUser, BigDecimal.ZERO, LocalDate.now());
        when(transactionRepository.findById(testTransaction.getId())).thenReturn(Optional.of(testTransaction));

        assertThrows(TransactionValidationException.class,
                () -> transactionService.updateTransaction(testTransaction.getId(), replacement));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_ShouldThrowNotFound_WhenMissing() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.updateTransaction(99L, testTransaction));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void deleteTransaction_ShouldThrowNotFound_WhenMissing() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.deleteTransaction(99L));
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void mapToEntity_ShouldUseOwnedCategory_AndCurrentUser() {
        CreateTransactionDto dto = new CreateTransactionDto(
                testTransactionType, "testdescription", testCategory.getId(), new BigDecimal("100.00"), LocalDate.now());
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(categoryService.getOwnedCategoryById(testCategory.getId(), testUser.getId())).thenReturn(testCategory);

        Transaction result = transactionService.mapToEntity(dto);

        assertEquals(testCategory, result.getCategory());
        assertEquals(testUser, result.getUser());
        verify(categoryService).getOwnedCategoryById(testCategory.getId(), testUser.getId());
    }

    @Test
    void mapToEntity_ShouldPropagateNotFound_WhenCategoryOwnedBySomeoneElse() {
        CreateTransactionDto dto = new CreateTransactionDto(
                testTransactionType, "testdescription", testCategory.getId(), new BigDecimal("100.00"), LocalDate.now());
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(categoryService.getOwnedCategoryById(testCategory.getId(), testUser.getId()))
                .thenThrow(new CategoryNotFoundException(testCategory.getId()));

        assertThrows(CategoryNotFoundException.class, () -> transactionService.mapToEntity(dto));
    }

    @Test
    void mapToEntity_ShouldNormalizeAmountToTwoDecimals_WhenAmountHasFewerDecimals() {
        CreateTransactionDto dto = new CreateTransactionDto(
                testTransactionType, "testdescription", testCategory.getId(), new BigDecimal("100"), LocalDate.now());
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(categoryService.getOwnedCategoryById(testCategory.getId(), testUser.getId())).thenReturn(testCategory);

        Transaction result = transactionService.mapToEntity(dto);

        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(2, result.getAmount().scale());
    }
}
