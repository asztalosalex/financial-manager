package hu.financial.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import hu.financial.repository.CategoryRepository;
import hu.financial.model.Category;
import hu.financial.model.User;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import hu.financial.dto.category.CategoryResponseDto;
import hu.financial.dto.category.CreateCategoryDto;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

  @Mock
  private UserService userService;

  @Mock
  private CategoryRepository categoryRepository;

  @InjectMocks
  private CategoryService categoryService;

  private Category testCategory;
  private User testUser;
  private CreateCategoryDto testCategoryDto;

  @BeforeEach
  void setUp() {
    testUser = new User("testuser", "password123", "test@example.com");
    testUser.setId(1L);
    testCategory = new Category("testcategory", "testdescription", testUser);
    testCategory.setId(1L);

    testCategoryDto = new CreateCategoryDto("testcategory", "testdescription");
  }

  @Test
  void createCategory_ShouldReturnCategory_WhenValidCategory() {

    // Arrange
    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    // Act
    Category result = categoryService.createCategory(testCategory);

    // Assert
    assertNotNull(result);
  }

  @Test
  void getCategoryById_ShouldReturnCategory_WhenValidCategory() {
    // Arrange
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));

    // Act
    Category result = categoryService.getCategoryById(testCategory.getId());

    // Assert
    assertNotNull(result);
  }

  @Test
  void updateCategory_ShouldReturnCategory_WhenValidCategory() {
    // Arrange
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    // Act
    Category result = categoryService.updateCategory(testCategory);

    // Assert
    assertNotNull(result);
  }

  @Test
  void deleteCategory_ShouldReturnVoid_WhenValidCategory() {
    // Arrange
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
    doNothing().when(categoryRepository).deleteById(testCategory.getId());

    // Act
    categoryService.deleteCategory(testCategory.getId());
  }

  @Test
  void getAllCategories_ShouldReturnListOfCategories_WhenCategoriesExist() {
    // Arrange
    when(categoryRepository.findAll()).thenReturn(Arrays.asList(testCategory));

    // Act
    List<Category> result = categoryService.getAllCategories();

    // Assert
    assertNotNull(result);
  }

  @Test
  void mapToDto_ShouldReturnCategoryResponseDto_WhenValidCategory() {

    // Act
    CategoryResponseDto result = categoryService.mapToDto(testCategory);

    // Assert
    assertNotNull(result);
  }

  @Test
  void mapToEntity_ShouldReturnCategory_WhenValidCategory() {

    // Act
    Category result = categoryService.mapToEntity(testCategoryDto);
    result.setUser(testUser);

    // Assert
    assertNotNull(result);
    assertEquals(testCategory.getName(), result.getName());
    assertEquals(testCategory.getDescription(), result.getDescription());
    assertEquals(testUser.getUsername(), result.getUser().getUsername());
  }

  @Test
  void getCategoriesByUserId_ShouldReturnListOfCategories_WhenCategoriesExist() {
    // Arrange
    when(categoryRepository.findByUserId(testUser.getId())).thenReturn(Arrays.asList(testCategory));

    // Act
    List<Category> result = categoryService.getCategoriesByUserId(testUser.getId());

    // Assert
    assertNotNull(result);
  }
}
