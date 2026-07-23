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

    @Operation(summary = "Create a new transaction")
    @PostMapping
    public ResponseEntity<TransactionResponseDto> createTransaction(@Valid @RequestBody CreateTransactionDto dto) {
        Transaction transaction = transactionService.mapToEntity(dto);
        Transaction savedTransaction = transactionService.createTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.mapToDto(savedTransaction));
    }

    @Operation(summary = "Get all transactions")
    @GetMapping
    public ResponseEntity<List<TransactionResponseDto>> getAllTransactions() {
        List<TransactionResponseDto> transactions = transactionService.getAllTransactions()
                .stream().map(transactionService::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }

    @Operation(summary = "Get a transaction by id")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransactionById(id);
        TransactionResponseDto responseDto = transaction == null ? null : transactionService.mapToDto(transaction);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "Update a transaction by id")
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(@PathVariable Long id, @Valid @RequestBody CreateTransactionDto dto) {
        Transaction updatedTransaction = transactionService.updateTransaction(id, transactionService.mapToEntity(dto));
        return ResponseEntity.ok(transactionService.mapToDto(updatedTransaction));
    }

    @Operation(summary = "Delete a transaction by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get transactions by user id")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionsByUserId(@PathVariable Long userId) {
        List<TransactionResponseDto> transactions = transactionService.getTransactionsByUserId(userId)
                .stream().map(transactionService::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }
}
