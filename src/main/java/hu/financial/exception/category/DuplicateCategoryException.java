package hu.financial.exception.category;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateCategoryException extends RuntimeException {
    
    public DuplicateCategoryException(String field, String value) {
        super("Duplicate " + field + " value: " + value);
    }
}
