package hu.financial.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.ToString;


public record RegisterUserDto(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        @ToString.Exclude
        String password,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
){}
