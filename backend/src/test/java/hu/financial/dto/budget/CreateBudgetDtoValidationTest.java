package hu.financial.dto.budget;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateBudgetDtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void amountWithThreeDecimals_IsRejected() {
        CreateBudgetDto dto = new CreateBudgetDto(new BigDecimal("2450.999"), LocalDate.now(), 1L);

        Set<ConstraintViolation<CreateBudgetDto>> violations = validator.validate(dto);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void amountWithTwoDecimals_IsAccepted() {
        CreateBudgetDto dto = new CreateBudgetDto(new BigDecimal("2450.99"), LocalDate.now(), 1L);

        Set<ConstraintViolation<CreateBudgetDto>> violations = validator.validate(dto);

        assertFalse(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }
}
