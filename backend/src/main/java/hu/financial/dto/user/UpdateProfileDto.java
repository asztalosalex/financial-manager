package hu.financial.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record UpdateProfileDto(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a well-formed email address")
        String email
){}
