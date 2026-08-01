package hu.financial.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import hu.financial.repository.TransactionRepository;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.exception.transaction.TransactionNotFoundException;
import hu.financial.exception.transaction.TransactionValidationException;
import hu.financial.dto.transaction.CreateTransactionDto;
import hu.financial.dto.transaction.TransactionResponseDto;

@Service
public class TransactionService {

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private CategoryService categoryService;

  @Autowired
  private UserService userService;

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
    validateTransactionForUpdate(existingTransaction, transaction);
    existingTransaction.setAmount(transaction.getAmount());
    existingTransaction.setDescription(transaction.getDescription());
    return transactionRepository.save(existingTransaction);
  }

  private void validateTransactionForCreation(Transaction transaction) {
    if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new TransactionValidationException("Transaction amount must be greater than 0");
    }
  }

  public List<Transaction> getTransactionsByUserId(Long userId) {
    return transactionRepository.findByUserId(userId);
  }

  @Transactional
  public void deleteTransaction(Long id) {
    Transaction existingTransaction = transactionRepository.findById(id)
        .orElseThrow(() -> new TransactionNotFoundException(id));
    transactionRepository.delete(existingTransaction);
  }

  private void validateTransactionForUpdate(Transaction existingTransaction, Transaction transaction) {
    if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new TransactionValidationException("Transaction amount must be greater than 0");
    }
  }

  public Transaction mapToEntity(CreateTransactionDto dto) {
    User currentUser = userService.getCurrentUser();
    Transaction transaction = new Transaction();
    transaction.setType(dto.getType());
    transaction.setDescription(dto.getDescription());
    transaction.setAmount(dto.getAmount().setScale(2, RoundingMode.HALF_UP));
    transaction.setDate(dto.getDate());
    transaction.setCategory(categoryService.getOwnedCategoryById(dto.getCategoryId(), currentUser.getId()));
    transaction.setUser(currentUser);
    return transaction;
  }

  public TransactionResponseDto mapToDto(Transaction transaction) {
    return new TransactionResponseDto(
        transaction.getId(),
        transaction.getType(),
        transaction.getDescription(),
        transaction.getCategory().getId(),
        transaction.getCategory().getName(),
        transaction.getAmount(),
        transaction.getDate());
  }

}
