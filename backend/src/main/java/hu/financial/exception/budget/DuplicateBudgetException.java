package hu.financial.exception.budget;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.time.LocalDate;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateBudgetException extends RuntimeException {

    public DuplicateBudgetException(Long categoryId, LocalDate month) {
        super("A budget for category " + categoryId + " and month " + month + " already exists");
    }
}
