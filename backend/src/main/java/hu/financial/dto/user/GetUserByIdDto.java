package hu.financial.dto.user;

import hu.financial.dto.category.CategoryResponseDto;
import java.util.List;


public record GetUserByIdDto(
        Long id,
        String userName,
        List<CategoryResponseDto> categories
){}
