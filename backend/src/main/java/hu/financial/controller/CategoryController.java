package hu.financial.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import hu.financial.service.CategoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import hu.financial.model.Category;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import hu.financial.dto.category.CreateCategoryDto;
import hu.financial.dto.category.CategoryResponseDto;
import java.util.stream.Collectors;
import hu.financial.service.UserService;
import hu.financial.exception.category.CategoryNotFoundException;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category", description = "Categories Handler")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Create a new category")
    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CreateCategoryDto dto) {
        Category category = categoryService.mapToEntity(dto);
        Category savedCategory = categoryService.createCategory(category);
        CategoryResponseDto responseDto = categoryService.mapToDto(savedCategory);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Operation(summary = "Get categories by user")
    @GetMapping("/user")
    public ResponseEntity<List<CategoryResponseDto>> getMyCategories() {
        Long userId = userService.getCurrentUser().getId();
        List<CategoryResponseDto> categories = categoryService.getCategoriesByUserId(userId)
        .stream().map(categoryService::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Update a category by id")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(@PathVariable Long id, @Valid @RequestBody CreateCategoryDto dto) {
        Long currentUserId = userService.getCurrentUser().getId();
        Category existingCategory = categoryService.getCategoryById(id);
        
        // Validate ownership
        if (!existingCategory.getUser().getId().equals(currentUserId)) {
            throw new CategoryNotFoundException("Category not found or you don't have permission to update it");
        }
        
        existingCategory.setName(dto.getName());
        existingCategory.setDescription(dto.getDescription());
        Category updatedCategory = categoryService.updateCategory(existingCategory);
        CategoryResponseDto responseDto = categoryService.mapToDto(updatedCategory);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "Delete a category by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        Long currentUserId = userService.getCurrentUser().getId();
        categoryService.deleteCategory(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    
}
