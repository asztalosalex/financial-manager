package hu.financial.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import hu.financial.repository.TransactionRepository;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.model.Category;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.time.LocalDate;
import hu.financial.model.enums.TransactionType;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

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
        testTransaction = new Transaction(1L, testTransactionType, "testdescription", testCategory, testUser, 100.0, LocalDate.now());
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
        doNothing().when(transactionRepository).deleteById(testTransaction.getId());

        // Act
        transactionService.deleteTransaction(testTransaction.getId());

        // Assert
        verify(transactionRepository, times(1)).deleteById(testTransaction.getId());
    }

    @Test
    void getTransactionsByUserId_ShouldReturnListOfTransactions_WhenTransactionsExist() {
        // Arrange
        when(transactionRepository.findByUserId(testUser.getId())).thenReturn(Arrays.asList(testTransaction));

        // Act
        List<Transaction> result = transactionService.getTransactionsByUserId(testUser.getId());

        // Assert
        assertNotNull(result);
        assertEquals(Arrays.asList(testTransaction), result);
        verify(transactionRepository, times(1)).findByUserId(testUser.getId());
    }
}
