package hu.financial.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestParameterException extends RuntimeException {

    private final String parameter;

    public InvalidRequestParameterException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
