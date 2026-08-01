package hu.financial.dto.transaction;

import hu.financial.model.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    @DecimalMin(value = "0.0", inclusive = false, message = "Transaction amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Transaction amount must have at most 17 integer digits and 2 fraction digits")
    private BigDecimal amount;

    @NotNull(message = "Transaction date is required")
    private LocalDate date;
}
