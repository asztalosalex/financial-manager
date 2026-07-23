package hu.financial.mapper;

import hu.financial.dto.category.CategoryResponseDto;
import hu.financial.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {


    public CategoryResponseDto toDto(Category category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
