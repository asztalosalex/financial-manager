package hu.financial.dto.budget;

import java.math.BigDecimal;
import java.time.LocalDate;



public record BudgetResponseDto(

        Long id,
        BigDecimal amount,
        LocalDate month,
        Long categoryId,
        String categoryName
){}
