package hu.financial.mapper;

import hu.financial.dto.user.GetUserByIdDto;
import hu.financial.dto.user.UpdateProfileDto;
import hu.financial.dto.user.UserResponseDto;
import hu.financial.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  private final CategoryMapper categoryMapper;

  public UserMapper(CategoryMapper categoryMapper) {
    this.categoryMapper = categoryMapper;
  }

  public UserResponseDto mapToDto(User user) {
    return new UserResponseDto(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getCreatedAt(),
        user.getLastLogin());
  }

  public GetUserByIdDto toGetUserByIdDto(User user) {

    return new GetUserByIdDto(
        user.getId(),
        user.getUsername(),
        user.getCategories()
            .stream()
            .map(categoryMapper::toDto)
            .toList());
  }

  public UpdateProfileDto mapToUpdateProfileDto(User user) {
    return new UpdateProfileDto(
        user.getUsername(),
        user.getEmail());
  }

}
