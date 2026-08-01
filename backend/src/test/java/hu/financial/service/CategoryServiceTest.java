package hu.financial.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import hu.financial.repository.CategoryRepository;
import hu.financial.model.Category;
import hu.financial.model.User;
import hu.financial.exception.category.CategoryNotFoundException;
import hu.financial.exception.category.DuplicateCategoryException;
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
  void createCategory_ShouldReturnSaved_WhenNameUnique() {
    when(categoryRepository.findByUserAndName(testCategory.getUser(), testCategory.getName())).thenReturn(null);
    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.createCategory(testCategory);

    assertEquals(testCategory, result);
    verify(categoryRepository).save(testCategory);
  }

  @Test
  void createCategory_ShouldThrowDuplicate_WhenNameExists() {
    when(categoryRepository.findByUserAndName(testCategory.getUser(), testCategory.getName())).thenReturn(testCategory);

    assertThrows(DuplicateCategoryException.class, () -> categoryService.createCategory(testCategory));
    verify(categoryRepository, never()).save(any(Category.class));
  }

  @Test
  void getCategoryById_ShouldReturnCategory_WhenExists() {
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));

    Category result = categoryService.getCategoryById(testCategory.getId());

    assertEquals(testCategory, result);
    verify(categoryRepository).findById(testCategory.getId());
  }

  @Test
  void getCategoryById_ShouldThrowNotFound_WhenMissing() {
    when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(99L));
  }

  @Test
  void updateCategory_ShouldReturnUpdated_WhenExists() {
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.updateCategory(testCategory);

    assertEquals(testCategory, result);
    verify(categoryRepository).findById(testCategory.getId());
    verify(categoryRepository).save(testCategory);
  }

  @Test
  void updateCategory_ShouldThrowDuplicate_WhenRenamedToNameAlreadyUsedByCurrentUser() {
    Category renamed = new Category("newname", "testdescription", testUser);
    renamed.setId(testCategory.getId());
    Category colliding = new Category("newname", "other", testUser);

    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
    when(categoryRepository.findByUserAndName(testUser, "newname")).thenReturn(colliding);

    assertThrows(DuplicateCategoryException.class, () -> categoryService.updateCategory(renamed));
    verify(categoryRepository, never()).save(any(Category.class));
  }

  @Test
  void updateCategory_ShouldThrowNotFound_WhenMissing() {
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.empty());

    assertThrows(CategoryNotFoundException.class, () -> categoryService.updateCategory(testCategory));
    verify(categoryRepository, never()).save(any(Category.class));
  }

  @Test
  void deleteCategory_ShouldDelete_WhenExistsAndOwnedByUser() {
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
    doNothing().when(categoryRepository).deleteById(testCategory.getId());

    categoryService.deleteCategory(testCategory.getId(), testUser.getId());

    verify(categoryRepository).deleteById(testCategory.getId());
  }

  @Test
  void deleteCategory_ShouldThrowNotFound_WhenMissing() {
    when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(99L, testUser.getId()));
    verify(categoryRepository, never()).deleteById(any());
  }

  @Test
  void deleteCategory_ShouldThrowNotFound_WhenOwnedBySomeoneElse() {
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));

    assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(testCategory.getId(), 99L));
    verify(categoryRepository, never()).deleteById(any());
  }

  @Test
  void getOwnedCategoryById_ShouldReturnCategory_WhenOwnedByUser() {
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));

    Category result = categoryService.getOwnedCategoryById(testCategory.getId(), testUser.getId());

    assertEquals(testCategory, result);
  }

  @Test
  void getOwnedCategoryById_ShouldThrowNotFound_WhenOwnedBySomeoneElse() {
    when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));

    assertThrows(CategoryNotFoundException.class,
        () -> categoryService.getOwnedCategoryById(testCategory.getId(), 99L));
  }

  @Test
  void getOwnedCategoryById_ShouldThrowNotFound_WhenMissing() {
    when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(CategoryNotFoundException.class,
        () -> categoryService.getOwnedCategoryById(99L, testUser.getId()));
  }

  @Test
  void getAllCategories_ShouldReturnList_WhenCategoriesExist() {
    when(categoryRepository.findAll()).thenReturn(Arrays.asList(testCategory));

    List<Category> result = categoryService.getAllCategories();

    assertEquals(Arrays.asList(testCategory), result);
    verify(categoryRepository, atLeastOnce()).findAll();
  }

  @Test
  void getCategoriesByUserId_ShouldReturnList_WhenCategoriesExist() {
    when(categoryRepository.findByUserId(testUser.getId())).thenReturn(Arrays.asList(testCategory));

    List<Category> result = categoryService.getCategoriesByUserId(testUser.getId());

    assertEquals(Arrays.asList(testCategory), result);
    verify(categoryRepository).findByUserId(testUser.getId());
  }

  @Test
  void mapToDto_ShouldMapAllFields() {
    CategoryResponseDto result = categoryService.mapToDto(testCategory);

    assertEquals(testCategory.getId(), result.getId());
    assertEquals(testCategory.getName(), result.getName());
    assertEquals(testCategory.getDescription(), result.getDescription());
  }

  @Test
  void mapToEntity_ShouldMapFields_AndSetCurrentUser() {
    when(userService.getCurrentUser()).thenReturn(testUser);

    Category result = categoryService.mapToEntity(testCategoryDto);

    assertEquals(testCategoryDto.getName(), result.getName());
    assertEquals(testCategoryDto.getDescription(), result.getDescription());
    assertEquals(testUser, result.getUser());
  }
}
