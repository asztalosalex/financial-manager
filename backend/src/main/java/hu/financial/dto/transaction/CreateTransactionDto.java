package hu.financial.dto.transaction;

import hu.financial.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionDto {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    private String description;

    @NotNull(message = "Category id is required")
    private Long categoryId;

    @NotNull(message = "Transaction amount is required")
    @Positive(message = "Transaction amount must be greater than 0")
    private Double amount;

    @NotNull(message = "Transaction date is required")
    private LocalDate date;
}
