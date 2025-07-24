package hu.financial.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import hu.financial.repository.TransactionRepository;
import hu.financial.model.Transaction;
import hu.financial.exception.transaction.TransactionNotFoundException;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction createTransaction(Transaction transaction) {
        validateTransactionForCreation(transaction);
        return transactionRepository.save(transaction);
    }


    public List<Transaction> getAllTransactions() {
        if (transactionRepository.findAll().isEmpty()) {
            throw new TransactionNotFoundException("No transactions found");
        }
        return transactionRepository.findAll();
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id).orElse(null);
    }

    public Transaction updateTransaction(Long id, Transaction transaction) {
        Transaction existingTransaction = transactionRepository.findById(id).orElse(null);
        validateTransactionForUpdate(existingTransaction, transaction);
        existingTransaction.setAmount(transaction.getAmount());
        existingTransaction.setDescription(transaction.getDescription());
        return transactionRepository.save(existingTransaction);
    }

    private void validateTransactionForCreation(Transaction transaction) {
        if (transaction.getAmount() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than 0");
        }
    }

    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserId(userId);
    }

    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }

    private void validateTransactionForUpdate(Transaction existingTransaction, Transaction transaction) {
        if (transaction.getAmount() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than 0");
        }
    }

    
}