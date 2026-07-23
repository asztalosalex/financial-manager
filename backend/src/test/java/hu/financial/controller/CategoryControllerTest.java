package hu.financial.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hu.financial.service.CategoryService;
import hu.financial.service.UserService;
import hu.financial.model.User;
import hu.financial.dto.category.CreateCategoryDto;
import hu.financial.dto.category.CategoryResponseDto;
import hu.financial.model.Category;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
public class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;
    
    @Mock
    private UserService userService;

    @InjectMocks
    private CategoryController categoryController;


    private User testUser;
    private CreateCategoryDto createCategoryDto;
    private Category testCategory;
    private CategoryResponseDto categoryResponseDto;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", "test@example.com");
        testUser.setId(1L);

        createCategoryDto = new CreateCategoryDto("testcategory", "testdescription");
        testCategory = new Category("testcategory", "testdescription", testUser);
        testCategory.setId(1L);

        categoryResponseDto = new CategoryResponseDto(
            testCategory.getId(), 
            testCategory.getName(), 
            testCategory.getDescription());
    }

    @Test
    void createCategory_ShouldReturnCategory_WhenValidCategory() {
        // Arrange
    when(categoryService.mapToEntity(any(CreateCategoryDto.class))).thenReturn(testCategory);
    when(categoryService.createCategory(any(Category.class))).thenReturn(testCategory);
    when(categoryService.mapToDto(any(Category.class)))
            .thenReturn(new CategoryResponseDto(testCategory.getId(), testCategory.getName(), testCategory.getDescription()));

    // Act
    ResponseEntity<CategoryResponseDto> response = categoryController.createCategory(createCategoryDto);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(testCategory.getName(), response.getBody().getName());
    assertEquals(testCategory.getDescription(), response.getBody().getDescription());

    verify(categoryService).mapToEntity(any(CreateCategoryDto.class));
    verify(categoryService).createCategory(any(Category.class));
    verify(categoryService).mapToDto(any(Category.class));
    }

    @Test
    void getMyCategories_ShouldReturnCategories_WhenValidUser() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(categoryService.getCategoriesByUserId(testUser.getId())).thenReturn(Arrays.asList(testCategory));
        when(categoryService.mapToDto(any(Category.class))).thenReturn(new CategoryResponseDto(testCategory.getId(), testCategory.getName(), testCategory.getDescription()));

        // Act
        ResponseEntity<List<CategoryResponseDto>> response = categoryController.getMyCategories();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCategory.getName(), response.getBody().get(0).getName());
        assertEquals(testCategory.getDescription(), response.getBody().get(0).getDescription());

        verify(categoryService).getCategoriesByUserId(testUser.getId());
        verify(categoryService).mapToDto(testCategory);
    }

    @Test
    void updateCategory_ShouldReturnCategory_WhenValidCategory() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(categoryService.getCategoryById(testCategory.getId())).thenReturn(testCategory);
        when(categoryService.updateCategory(any(Category.class))).thenReturn(testCategory);
        when(categoryService.mapToDto(any(Category.class))).thenReturn(categoryResponseDto);

        // Act
        ResponseEntity<CategoryResponseDto> response = categoryController.updateCategory(testCategory.getId(), createCategoryDto);
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCategory.getName(), response.getBody().getName());
        assertEquals(testCategory.getDescription(), response.getBody().getDescription());

        verify(categoryService).getCategoryById(testCategory.getId());
        verify(categoryService).mapToDto(testCategory);
    }

    @Test
    void deleteCategory_ShouldReturnNoContent_WhenValidCategory() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        doNothing().when(categoryService).deleteCategory(testCategory.getId(), testUser.getId());

        // Act
        ResponseEntity<Void> response = categoryController.deleteCategory(testCategory.getId());
        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
