package hu.financial.controller;

import hu.financial.dto.user.GetUserByIdDto;
import hu.financial.exception.user.UserNotFoundException;
import org.springframework.web.bind.annotation.RestController;
import hu.financial.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import hu.financial.model.User;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import hu.financial.service.JwtService;
import hu.financial.dto.user.ChangePasswordRequestDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import hu.financial.dto.user.UserResponseDto;
import hu.financial.dto.user.UpdateProfileDto;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "Users Handler")
public class UserController {

  private UserService userService;
  private JwtService jwtService;

  public UserController(UserService userService, JwtService jwtService) {
    this.userService = userService;
    this.jwtService = jwtService;
  }

  @Operation(summary = "Get all users")
  @GetMapping
  public ResponseEntity<List<User>> getAllUsers() {
    List<User> users = userService.getAllUsers();
    if (users.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(users);
  }

  @Operation(summary = "Get a user by id")
  @GetMapping("/{id}")
  public ResponseEntity<GetUserByIdDto> getUserById(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(userService.getUserByIdDto(id));
    } catch (UserNotFoundException e) {
      return ResponseEntity.status(404).build();
    }
  }

  @Operation(summary = "Update a user by id")
  @PutMapping("/{id}")
  public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateProfileDto updateProfileDto,
      @Valid @RequestHeader("Authorization") String authHeader) {
    try {
      String token = authHeader.substring(7);
      String username = jwtService.extractUsername(token);
      User currentUser = userService.getUserByUsername(username);

      if (!currentUser.getId().equals(id)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body("You can only update your own account");
      }

      User updatedUser = userService.updateUser(id, updateProfileDto);
      return ResponseEntity.ok(userService.updateProfileDto(updatedUser));

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed");
    }
  }

  @Operation(summary = "Delete a user by id")
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
    try {
      String token = authHeader.substring(7);
      String username = jwtService.extractUsername(token);
      User currentUser = userService.getUserByUsername(username);

      // Users can only delete their own account
      if (!currentUser.getId().equals(id)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body("You can only delete your own account");
      }

      userService.deleteUser(id);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Authentication failed");
    }
  }

  @Operation(summary = "Get current user profile")
  @GetMapping("/profile")
  public ResponseEntity<UserResponseDto> getCurrentUserProfile(Authentication authentication) {
    try {
      User user = (User) authentication.getPrincipal();
      UserResponseDto dto = userService.mapToUserProfileDto(user);
      return ResponseEntity.ok(dto);
    } catch (Exception e) {
      return ResponseEntity.status(401).build();
    }
  }

  @Operation(summary = "Change user password")
  @PostMapping("/change-password")
  public ResponseEntity<?> changePassword(Authentication authentication,
      @Valid @RequestBody ChangePasswordRequestDto changePasswordRequest) {
    try {
      User user = (User) authentication.getPrincipal();

      // Verify current password
      if (!userService.verifyPassword(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Current password is incorrect");
      }

      // Update password
      userService.changePassword(user.getId(), changePasswordRequest.getNewPassword());

      return ResponseEntity.ok().body("Password changed successfully");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to change password");
    }
  }

  @Operation(summary = "Get the number of users")
  @GetMapping("/count")
  public long getNumberOfUsers() {
    return userService.countUsers();
  }
}
