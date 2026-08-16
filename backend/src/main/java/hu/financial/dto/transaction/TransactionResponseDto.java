package hu.financial.dto.transaction;

import hu.financial.model.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;


public record TransactionResponseDto(

     Long id,
     TransactionType type,
     String description,
     Long categoryId,
     String categoryName,
     BigDecimal amount,
     LocalDate date
){}