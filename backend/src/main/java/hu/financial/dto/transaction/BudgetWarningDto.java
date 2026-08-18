package hu.financial.dto.transaction;

import hu.financial.dto.report.BudgetStatusItemDto;
import java.math.BigDecimal;

public record BudgetWarningDto(
    Long categoryId,
    String categoryName,
    BigDecimal budgeted,
    BigDecimal spent,
    BigDecimal remaining,
    BigDecimal percentageUsed) {

  public static BudgetWarningDto from(BudgetStatusItemDto status) {
    return new BudgetWarningDto(
        status.categoryId(), status.categoryName(), status.budgeted(),
        status.spent(), status.remaining(), status.percentageUsed());
  }
}
