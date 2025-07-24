package hu.financial.exception.budget;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateBudgetException extends RuntimeException {

    public DuplicateBudgetException(String message) {
        super(message);
    }

    public DuplicateBudgetException(String field, String value) {
        super("Budget with " + field + " '" + value + "' already exists");
    }
}
