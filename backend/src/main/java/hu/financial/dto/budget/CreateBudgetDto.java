package hu.financial.dto.budget;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBudgetDto {

    @NotNull(message = "Budget amount is required")
    @Positive(message = "Budget amount must be greater than 0")
    private Double amount;

    @NotNull(message = "Budget month is required")
    private YearMonth month;

    @NotNull(message = "Category id is required")
    private Long categoryId;
}
