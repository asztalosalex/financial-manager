package hu.financial.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponseDto {

    private Long id;
    private Double amount;
    private YearMonth month;
    private Long categoryId;
    private String categoryName;
}
