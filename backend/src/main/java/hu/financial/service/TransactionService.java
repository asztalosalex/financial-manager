package hu.financial.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import hu.financial.repository.TransactionRepository;
import hu.financial.repository.spec.TransactionSpecifications;
import hu.financial.dto.transaction.TransactionFilter;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.exception.transaction.TransactionNotFoundException;
import hu.financial.exception.transaction.TransactionValidationException;
import hu.financial.dto.transaction.CreateTransactionDto;
import hu.financial.dto.transaction.TransactionResponseDto;
import hu.financial.dto.transaction.BudgetWarningDto;
import hu.financial.model.enums.TransactionType;

@Service
public class TransactionService {

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private CategoryService categoryService;

  @Autowired
  private UserService userService;

  @Autowired
  private ReportService reportService;

  @Transactional
  public Transaction createTransaction(Transaction transaction) {
    validateTransactionForCreation(transaction);
    return transactionRepository.save(transaction);
  }

  public List<Transaction> getAllTransactions() {
    List<Transaction> transactions = transactionRepository.findAll();
    if (transactions.isEmpty()) {
      throw new TransactionNotFoundException("No transactions found");
    }
    return transactions;
  }

  public Transaction getTransactionById(Long id) {
    return transactionRepository.findById(id)
        .orElseThrow(() -> new TransactionNotFoundException(id));
  }

  @Transactional
  public Transaction updateTransaction(Long id, Transaction transaction) {
    Transaction existingTransaction = transactionRepository.findById(id)
        .orElseThrow(() -> new TransactionNotFoundException(id));
    validateAmount(transaction.getAmount());
    existingTransaction.setType(transaction.getType());
    existingTransaction.setDescription(transaction.getDescription());
    existingTransaction.setCategory(transaction.getCategory());
    existingTransaction.setAmount(transaction.getAmount());
    existingTransaction.setDate(transaction.getDate());
    return transactionRepository.save(existingTransaction);
  }

  private void validateTransactionForCreation(Transaction transaction) {
    validateAmount(transaction.getAmount());
  }

  private void validateAmount(BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new TransactionValidationException("Transaction amount must be greater than 0");
    }
  }

  public Page<Transaction> getTransactionsByUserId(Long userId, TransactionFilter filter, Pageable pageable) {
    return transactionRepository.findAll(TransactionSpecifications.ownedBy(userId, filter), pageable);
  }

  @Transactional
  public void deleteTransaction(Long id) {
    Transaction existingTransaction = transactionRepository.findById(id)
        .orElseThrow(() -> new TransactionNotFoundException(id));
    transactionRepository.delete(existingTransaction);
  }

  public Transaction mapToEntity(CreateTransactionDto dto) {
    User currentUser = userService.getCurrentUser();
    Transaction transaction = new Transaction();
    transaction.setType(dto.type());
    transaction.setDescription(dto.description());
    transaction.setAmount(dto.amount().setScale(2, RoundingMode.HALF_UP));
    transaction.setDate(dto.date());
    transaction.setCategory(categoryService.getOwnedCategoryById(dto.categoryId(), currentUser.getId()));
    transaction.setUser(currentUser);
    return transaction;
  }

  public TransactionResponseDto mapToDtoWithBudgetWarning(Transaction transaction) {
    return mapToDto(transaction, resolveBudgetWarning(transaction));
  }

  public TransactionResponseDto mapToDto(Transaction transaction) {
    return mapToDto(transaction, null);
  }

  private TransactionResponseDto mapToDto(Transaction transaction, BudgetWarningDto budgetWarning) {
    return new TransactionResponseDto(
        transaction.getId(),
        transaction.getType(),
        transaction.getDescription(),
        transaction.getCategory().getId(),
        transaction.getCategory().getName(),
        transaction.getAmount(),
        transaction.getDate(),
        budgetWarning);
  }

  private BudgetWarningDto resolveBudgetWarning(Transaction transaction) {
    if (transaction.getType() != TransactionType.EXPENSE) {
      return null;
    }
    YearMonth month = YearMonth.from(transaction.getDate());
    return reportService.categoryBudgetStatus(transaction.getUser().getId(), transaction.getCategory().getId(), month)
        .filter(status -> status.remaining().signum() < 0)
        .map(BudgetWarningDto::from)
        .orElse(null);
  }

}
