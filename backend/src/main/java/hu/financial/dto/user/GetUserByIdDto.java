package hu.financial.dto.user;

import hu.financial.dto.category.CategoryResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetUserByIdDto {

  private Long id;
  private String userName;
  private List<CategoryResponseDto> categories;
}
