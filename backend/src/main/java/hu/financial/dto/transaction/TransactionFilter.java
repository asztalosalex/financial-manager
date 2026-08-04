package hu.financial.dto.transaction;

import java.time.LocalDate;

import hu.financial.exception.InvalidRequestParameterException;
import hu.financial.model.enums.TransactionType;

public record TransactionFilter(LocalDate from, LocalDate to, Long categoryId, TransactionType type) {

    public TransactionFilter {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestParameterException("from", "must not be after to");
        }
    }

    public static TransactionFilter unfiltered() {
        return new TransactionFilter(null, null, null, null);
    }
}
