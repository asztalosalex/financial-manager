package hu.financial.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserResponseDto {
  private Long id;
  private String username;
  private String email;
  private LocalDateTime createdAt;
  private LocalDateTime lastLogin;

  public UserResponseDto(Long expiresIn, String message) {

  }

}
