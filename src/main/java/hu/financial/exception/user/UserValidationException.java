package hu.financial.exception.user;

public class UserValidationException extends RuntimeException {
    
    public UserValidationException(String message) {
        super(message);
    }
} 