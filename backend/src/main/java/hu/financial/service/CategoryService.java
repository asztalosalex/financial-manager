package hu.financial.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import hu.financial.repository.CategoryRepository;
import hu.financial.model.Category;
import hu.financial.exception.category.DuplicateCategoryException;
import hu.financial.exception.category.CategoryNotFoundException;
import hu.financial.dto.category.CategoryResponseDto;
import hu.financial.dto.category.CreateCategoryDto;

@Service
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserService userService;
    
    public Category createCategory(Category category) {
        validateCategoryForCreation(category);
        return categoryRepository.save(category);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public Category updateCategory(Category category) {
        Category existingCategory = categoryRepository.findById(category.getId()).orElse(null); 
        validateCategoryForUpdate(existingCategory, category);
        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        return categoryRepository.save(existingCategory);
    }

    public void deleteCategory(Long id) {
        Category existingCategory = categoryRepository.findById(id).orElse(null);
        if (existingCategory == null) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
    }

    public void deleteCategory(Long id, Long userId) {
        Category existingCategory = categoryRepository.findById(id).orElse(null);
        if (existingCategory == null) {
            throw new CategoryNotFoundException(id);
        }
        if (!existingCategory.getUser().getId().equals(userId)) {
            throw new CategoryNotFoundException("Category not found or you don't have permission to delete it");
        }
        categoryRepository.deleteById(id);
    }

    public List<Category> getAllCategories() {
        if (categoryRepository.findAll().isEmpty()) {
            throw new CategoryNotFoundException("No categories found");
        }
        else {
            return categoryRepository.findAll();
        }
    }

    private void validateCategoryForCreation(Category category) {
        if (categoryRepository.findByName(category.getName()) != null) {
            throw new DuplicateCategoryException("name", category.getName());
        }
    }

    private void validateCategoryForUpdate(Category existingCategory, Category category) {
        if (!existingCategory.getName().equals(category.getName())) {
            if (categoryRepository.findByName(category.getName()) != null) {
                throw new DuplicateCategoryException("name", category.getName());
            }
        }
    }

    public CategoryResponseDto mapToDto(Category category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }

    public Category mapToEntity(CreateCategoryDto createCategoryDto) {
        Category category = new Category();
        category.setName(createCategoryDto.getName());
        category.setDescription(createCategoryDto.getDescription());
        category.setUser(userService.getCurrentUser());
        return category;
    }


    public List<Category> getCategoriesByUserId(Long userId) {
        return categoryRepository.findByUserId(userId);
    }
}
