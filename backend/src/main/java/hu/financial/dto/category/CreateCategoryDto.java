package hu.financial.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record CreateCategoryDto(

        @NotBlank(message = "Category name is required")
        @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
        String name,

        @NotBlank(message = "Category description is required")
        @Size(min = 5, max = 200, message = "Category description must be between 5 and 200 characters")
        String description
){}