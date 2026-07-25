package hu.financial.exception.budget;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException(String message) {
        super(message);
    }

    public BudgetNotFoundException(Long id) {
        super("Budget not found with id: " + id);
    }
}
