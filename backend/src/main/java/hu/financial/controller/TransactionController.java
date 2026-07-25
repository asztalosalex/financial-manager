package hu.financial.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import hu.financial.model.Transaction;
import hu.financial.service.TransactionService;
import hu.financial.service.UserService;
import hu.financial.exception.transaction.TransactionNotFoundException;
import hu.financial.dto.transaction.CreateTransactionDto;
import hu.financial.dto.transaction.TransactionResponseDto;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction", description = "Transactions Handler")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Create a new transaction")
    @PostMapping
    public ResponseEntity<TransactionResponseDto> createTransaction(@Valid @RequestBody CreateTransactionDto dto) {
        Transaction transaction = transactionService.mapToEntity(dto);
        Transaction savedTransaction = transactionService.createTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.mapToDto(savedTransaction));
    }

    @Operation(summary = "Get the current user's transactions")
    @GetMapping
    public ResponseEntity<List<TransactionResponseDto>> getMyTransactions() {
        Long userId = userService.getCurrentUser().getId();
        List<TransactionResponseDto> transactions = transactionService.getTransactionsByUserId(userId)
                .stream().map(transactionService::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Get one of the current user's transactions by id")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long id) {
        Transaction transaction = getOwnedTransaction(id);
        return ResponseEntity.ok(transactionService.mapToDto(transaction));
    }

    @Operation(summary = "Update one of the current user's transactions by id")
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(@PathVariable Long id, @Valid @RequestBody CreateTransactionDto dto) {
        getOwnedTransaction(id);
        Transaction updatedTransaction = transactionService.updateTransaction(id, transactionService.mapToEntity(dto));
        return ResponseEntity.ok(transactionService.mapToDto(updatedTransaction));
    }

    @Operation(summary = "Delete one of the current user's transactions by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        getOwnedTransaction(id);
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    private Transaction getOwnedTransaction(Long id) {
        Long userId = userService.getCurrentUser().getId();
        Transaction transaction = transactionService.getTransactionById(id);
        if (transaction == null || !transaction.getUser().getId().equals(userId)) {
            throw new TransactionNotFoundException(id);
        }
        return transaction;
    }
}
