package hu.financial.web;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;

import hu.financial.exception.InvalidRequestParameterException;

public final class SortWhitelist {

    public static final String PARAMETER_NAME = "sort";

    private static final String SEPARATOR = ",";

    private final Set<String> allowedFields;

    private final Sort.Order tieBreaker;

    private SortWhitelist(Set<String> allowedFields, Sort.Order tieBreaker) {
        this.allowedFields = allowedFields;
        this.tieBreaker = tieBreaker;
    }

    public static SortWhitelist of(List<String> allowedFields, Sort.Order tieBreaker) {
        if (allowedFields == null || allowedFields.isEmpty()) {
            throw new IllegalArgumentException("A sort whitelist needs at least one allowed field");
        }
        if (tieBreaker == null) {
            throw new IllegalArgumentException("A sort whitelist needs a tie breaker order");
        }
        return new SortWhitelist(new LinkedHashSet<>(allowedFields), tieBreaker);
    }

    public Sort toSort(String sort) {
        if (sort == null || sort.isBlank()) {
            throw invalid();
        }
        String[] parts = sort.split(SEPARATOR, -1);
        if (parts.length > 2) {
            throw invalid();
        }
        String field = parts[0].trim();
        if (!allowedFields.contains(field)) {
            throw invalid();
        }
        Sort.Direction direction = parts.length == 2 ? direction(parts[1].trim()) : Sort.Direction.ASC;
        Sort primary = Sort.by(direction, field);
        if (field.equals(tieBreaker.getProperty())) {
            return primary;
        }
        return primary.and(Sort.by(tieBreaker));
    }

    private Sort.Direction direction(String value) {
        return Sort.Direction.fromOptionalString(value).orElseThrow(this::invalid);
    }

    private InvalidRequestParameterException invalid() {
        return new InvalidRequestParameterException(PARAMETER_NAME,
                "must be one of " + String.join(", ", allowedFields) + " optionally followed by ,asc or ,desc");
    }
}
