package hu.financial.controller;

import hu.financial.dto.user.GetUserByIdDto;
import hu.financial.model.User;
import hu.financial.service.UserService;
import hu.financial.service.AuthenticationService;
import hu.financial.service.JwtService;
import hu.financial.exception.user.UserNotFoundException;
import hu.financial.dto.user.UserResponseDto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import hu.financial.dto.user.UpdateProfileDto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock
  private UserService userService;

  @Mock
  private AuthenticationService authenticationService;

  @Mock
  private JwtService jwtService;

  @Mock
  private UserResponseDto userResponseDto;

  @InjectMocks
  private UserController userController;

  @Test
  void getUserById_UserNotFound_Returns404() {
    Long userId = 999L;
    User currentUser = new User("testuser", "password", "test@example.com");
    currentUser.setId(userId);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(currentUser);

    when(userService.getUserByIdDto(userId))
        .thenThrow(new UserNotFoundException(userId));

    ResponseEntity<GetUserByIdDto> response = userController.getUserById(userId, authentication);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getUserById_ForeignId_Returns404_AndDoesNotExposeOtherUsersData() {
    Long ownId = 1L;
    Long foreignId = 2L;
    User currentUser = new User("testuser", "password", "test@example.com");
    currentUser.setId(ownId);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(currentUser);

    ResponseEntity<GetUserByIdDto> response = userController.getUserById(foreignId, authentication);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNull(response.getBody());
    verify(userService, never()).getUserByIdDto(any());
  }

  @Test
  void getUserById_OwnId_ReturnsOwnData() {
    Long ownId = 1L;
    User currentUser = new User("testuser", "password", "test@example.com");
    currentUser.setId(ownId);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(currentUser);

    GetUserByIdDto expectedDto = new GetUserByIdDto(ownId, "testuser", java.util.Collections.emptyList());
    when(userService.getUserByIdDto(ownId)).thenReturn(expectedDto);

    ResponseEntity<GetUserByIdDto> response = userController.getUserById(ownId, authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedDto, response.getBody());
  }

  @Test
  void updateUser_Success() {

    UpdateProfileDto updateProfileDto = new UpdateProfileDto("updateduser", "updated@example.com");
    Long userId = 1L;
    String authHeader = "Bearer validToken";
    String username = "testuser";
    User currentUser = new User("testuser", "password", "test@example.com");
    currentUser.setId(userId);

    when(jwtService.extractUsername("validToken")).thenReturn(username);
    when(userService.getUserByUsername(username)).thenReturn(currentUser);
    when(userService.updateUser(userId, updateProfileDto)).thenReturn(currentUser);

    // Act
    ResponseEntity<?> response = userController.updateUser(userId, updateProfileDto, authHeader);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void deleteUser_Success() {
    // Arrange
    Long userId = 1L;
    String authHeader = "Bearer validToken";
    String username = "testuser";
    User currentUser = new User("testuser", "password", "test@example.com");
    currentUser.setId(userId);

    when(jwtService.extractUsername("validToken")).thenReturn(username);
    when(userService.getUserByUsername(username)).thenReturn(currentUser);
    doNothing().when(userService).deleteUser(userId);

    // Act
    ResponseEntity<?> response = userController.deleteUser(userId, authHeader);

    // Assert
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(userService).deleteUser(userId);
  }

  @Test
  void deleteUser_Unauthorized_ReturnsUnauthorized() {
    // Arrange
    Long userId = 1L;
    String authHeader = "Bearer invalidToken";

    when(jwtService.extractUsername("invalidToken")).thenThrow(new RuntimeException("Invalid token"));

    // Act
    ResponseEntity<?> response = userController.deleteUser(userId, authHeader);

    // Assert
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals("Authentication failed", response.getBody());
  }

  @Test
  void deleteUser_Forbidden_ReturnsForbidden() {
    // Arrange
    Long userId = 1L;
    Long differentUserId = 2L;
    String authHeader = "Bearer validToken";
    String username = "testuser";
    User currentUser = new User("testuser", "password", "test@example.com");
    currentUser.setId(differentUserId); // Different user ID

    when(jwtService.extractUsername("validToken")).thenReturn(username);
    when(userService.getUserByUsername(username)).thenReturn(currentUser);

    // Act
    ResponseEntity<?> response = userController.deleteUser(userId, authHeader);

    // Assert
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals("You can only delete your own account", response.getBody());
  }
}
