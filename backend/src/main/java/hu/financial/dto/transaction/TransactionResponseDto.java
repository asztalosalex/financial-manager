package hu.financial.dto.transaction;

import hu.financial.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto {

    private Long id;
    private TransactionType type;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Double amount;
    private LocalDate date;
}
