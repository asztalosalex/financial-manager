package hu.financial.controller;

import hu.financial.dto.user.GetUserByIdDto;
import hu.financial.exception.user.UserNotFoundException;
import org.springframework.web.bind.annotation.RestController;
import hu.financial.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import hu.financial.model.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import hu.financial.dto.user.ChangePasswordRequestDto;
import org.springframework.web.bind.annotation.PostMapping;
import hu.financial.dto.user.UserResponseDto;
import hu.financial.dto.user.UpdateProfileDto;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "Users Handler")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @Operation(summary = "Get a user by id")
  @GetMapping("/{id}")
  public ResponseEntity<GetUserByIdDto> getUserById(@PathVariable Long id, Authentication authentication) {
    requireOwnAccount(id, authentication);
    return ResponseEntity.ok(userService.getUserByIdDto(id));
  }

  @Operation(summary = "Update a user by id")
  @PutMapping("/{id}")
  public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id,
      @Valid @RequestBody UpdateProfileDto updateProfileDto, Authentication authentication) {
    requireOwnAccount(id, authentication);
    User updatedUser = userService.updateUser(id, updateProfileDto);
    return ResponseEntity.ok(userService.mapToUserProfileDto(updatedUser));
  }

  @Operation(summary = "Delete a user by id")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
    requireOwnAccount(id, authentication);
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Get current user profile")
  @GetMapping("/profile")
  public ResponseEntity<UserResponseDto> getCurrentUserProfile(Authentication authentication) {
    return ResponseEntity.ok(userService.mapToUserProfileDto(currentUser(authentication)));
  }

  @Operation(summary = "Change user password")
  @PostMapping("/change-password")
  public ResponseEntity<Void> changePassword(Authentication authentication,
      @Valid @RequestBody ChangePasswordRequestDto changePasswordRequest) {
    userService.changePassword(currentUser(authentication), changePasswordRequest);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Get the number of users")
  @GetMapping("/count")
  public long getNumberOfUsers() {
    return userService.countUsers();
  }

  private User currentUser(Authentication authentication) {
    return (User) authentication.getPrincipal();
  }

  private void requireOwnAccount(Long id, Authentication authentication) {
    if (!currentUser(authentication).getId().equals(id)) {
      throw new UserNotFoundException(id);
    }
  }
}
