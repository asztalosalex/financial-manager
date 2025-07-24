package hu.financial.exception.transaction;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateTransactionException extends RuntimeException {
    
    public DuplicateTransactionException(String message) {
        super(message);
    }

    public DuplicateTransactionException(String field, String value) {
        super("Transaction with " + field + " '" + value + "' already exists");
    }
}
