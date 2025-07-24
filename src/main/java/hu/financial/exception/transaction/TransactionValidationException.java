package hu.financial.exception.transaction;

public class TransactionValidationException extends RuntimeException {

    public TransactionValidationException(String message) {
        super(message);   
    }
}
