package hu.financial.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import hu.financial.model.Transaction;
import hu.financial.model.enums.TransactionType;
import hu.financial.service.TransactionService;
import hu.financial.service.UserService;
import hu.financial.exception.transaction.TransactionNotFoundException;
import hu.financial.dto.common.PageResponse;
import hu.financial.dto.transaction.CreateTransactionDto;
import hu.financial.dto.transaction.TransactionFilter;
import hu.financial.dto.transaction.TransactionResponseDto;
import hu.financial.web.SortWhitelist;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Validated
@Tag(name = "Transaction", description = "Transactions Handler")
public class TransactionController {

    private static final String DEFAULT_PAGE = "0";

    private static final String DEFAULT_SIZE = "20";

    private static final String DEFAULT_SORT = "date,desc";

    private static final SortWhitelist SORT_WHITELIST =
            SortWhitelist.of(List.of("date", "amount", "id"), Sort.Order.desc("id"));

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
    public ResponseEntity<PageResponse<TransactionResponseDto>> getMyTransactions(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type) {
        Pageable pageable = PageRequest.of(page, size, SORT_WHITELIST.toSort(sort));
        TransactionFilter filter = new TransactionFilter(from, to, categoryId, type);
        Long userId = userService.getCurrentUser().getId();
        Page<Transaction> transactions = transactionService.getTransactionsByUserId(userId, filter, pageable);
        return ResponseEntity.ok(PageResponse.from(transactions.map(transactionService::mapToDto)));
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
