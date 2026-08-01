package hu.financial.dto.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserDtoValidationTest {

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
    void registerUserDto_ShouldHaveNoViolations_WhenAllFieldsValid() {
        RegisterUserDto dto = new RegisterUserDto("testuser", "password123", "test@example.com");

        Set<ConstraintViolation<RegisterUserDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void registerUserDto_ShouldHaveViolation_WhenUsernameBlank() {
        RegisterUserDto dto = new RegisterUserDto("", "password123", "test@example.com");

        Set<ConstraintViolation<RegisterUserDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void registerUserDto_ShouldHaveViolation_WhenEmailInvalid() {
        RegisterUserDto dto = new RegisterUserDto("testuser", "password123", "not-an-email");

        Set<ConstraintViolation<RegisterUserDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void registerUserDto_ShouldHaveViolation_WhenPasswordTooShort() {
        RegisterUserDto dto = new RegisterUserDto("testuser", "short", "test@example.com");

        Set<ConstraintViolation<RegisterUserDto>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("password", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void loginUserDto_ShouldHaveNoViolations_WhenAllFieldsValid() {
        LoginUserDto dto = new LoginUserDto("test@example.com", "password123");

        Set<ConstraintViolation<LoginUserDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void loginUserDto_ShouldHaveViolation_WhenEmailBlank() {
        LoginUserDto dto = new LoginUserDto("", "password123");

        Set<ConstraintViolation<LoginUserDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void loginUserDto_ShouldHaveViolation_WhenPasswordBlank() {
        LoginUserDto dto = new LoginUserDto("test@example.com", "");

        Set<ConstraintViolation<LoginUserDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void updateProfileDto_ShouldHaveNoViolations_WhenAllFieldsValid() {
        UpdateProfileDto dto = new UpdateProfileDto("testuser", "test@example.com");

        Set<ConstraintViolation<UpdateProfileDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void updateProfileDto_ShouldHaveViolation_WhenUsernameBlank() {
        UpdateProfileDto dto = new UpdateProfileDto("", "test@example.com");

        Set<ConstraintViolation<UpdateProfileDto>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("username", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void updateProfileDto_ShouldHaveViolation_WhenEmailInvalid() {
        UpdateProfileDto dto = new UpdateProfileDto("testuser", "not-an-email");

        Set<ConstraintViolation<UpdateProfileDto>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("email", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void updateProfileDto_ShouldHaveViolation_WhenEmailBlank() {
        UpdateProfileDto dto = new UpdateProfileDto("testuser", "");

        Set<ConstraintViolation<UpdateProfileDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }
}
