package hu.financial.dto.budget;

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
public class CreateBudgetDto {

    @NotNull(message = "Budget amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Budget amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Budget amount must have at most 17 integer digits and 2 fraction digits")
    private BigDecimal amount;

    @NotNull(message = "Budget month is required")
    private LocalDate month;

    @NotNull(message = "Category id is required")
    private Long categoryId;
}
