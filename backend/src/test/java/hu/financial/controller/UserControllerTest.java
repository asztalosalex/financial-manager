package hu.financial.controller;

import hu.financial.dto.user.ChangePasswordRequestDto;
import hu.financial.dto.user.GetUserByIdDto;
import hu.financial.model.User;
import hu.financial.service.UserService;
import hu.financial.exception.user.InvalidPasswordException;
import hu.financial.exception.user.UserNotFoundException;
import hu.financial.dto.user.UserResponseDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import hu.financial.dto.user.UpdateProfileDto;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock
  private UserService userService;

  @InjectMocks
  private UserController userController;

  private User currentUser;
  private Authentication authentication;

  @BeforeEach
  void setUp() {
    currentUser = new User("testuser", "password", "test@example.com");
    currentUser.setId(1L);
    authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(currentUser);
  }

  @Test
  void getUserById_UserNotFound_ThrowsUserNotFoundException() {
    when(userService.getUserByIdDto(1L)).thenThrow(new UserNotFoundException(1L));

    assertThrows(UserNotFoundException.class, () -> userController.getUserById(1L, authentication));
  }

  @Test
  void getUserById_ForeignId_ThrowsUserNotFoundException_AndDoesNotExposeOtherUsersData() {
    assertThrows(UserNotFoundException.class, () -> userController.getUserById(2L, authentication));

    verify(userService, never()).getUserByIdDto(any());
  }

  @Test
  void getUserById_OwnId_ReturnsOwnData() {
    GetUserByIdDto expectedDto = new GetUserByIdDto(1L, "testuser", java.util.Collections.emptyList());
    when(userService.getUserByIdDto(1L)).thenReturn(expectedDto);

    ResponseEntity<GetUserByIdDto> response = userController.getUserById(1L, authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedDto, response.getBody());
  }

  @Test
  void updateUser_OwnId_ReturnsUpdatedProfile() {
    UpdateProfileDto updateProfileDto = new UpdateProfileDto("updateduser", "updated@example.com");
    UserResponseDto expectedDto = new UserResponseDto(1L, "updateduser", "updated@example.com",
        LocalDateTime.now(), null);
    when(userService.updateUser(1L, updateProfileDto)).thenReturn(currentUser);
    when(userService.mapToUserProfileDto(currentUser)).thenReturn(expectedDto);

    ResponseEntity<UserResponseDto> response = userController.updateUser(1L, updateProfileDto, authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedDto, response.getBody());
  }

  @Test
  void updateUser_ForeignId_ThrowsUserNotFoundException() {
    UpdateProfileDto updateProfileDto = new UpdateProfileDto("updateduser", "updated@example.com");

    assertThrows(UserNotFoundException.class,
        () -> userController.updateUser(2L, updateProfileDto, authentication));

    verify(userService, never()).updateUser(any(), any());
  }

  @Test
  void deleteUser_OwnId_ReturnsNoContent() {
    doNothing().when(userService).deleteUser(1L);

    ResponseEntity<Void> response = userController.deleteUser(1L, authentication);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(userService).deleteUser(1L);
  }

  @Test
  void deleteUser_ForeignId_ThrowsUserNotFoundException() {
    assertThrows(UserNotFoundException.class, () -> userController.deleteUser(2L, authentication));

    verify(userService, never()).deleteUser(any());
  }

  @Test
  void changePassword_ValidRequest_ReturnsNoContent() {
    ChangePasswordRequestDto request = new ChangePasswordRequestDto("current123", "newpassword123");

    ResponseEntity<Void> response = userController.changePassword(authentication, request);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertNull(response.getBody());
    verify(userService).changePassword(currentUser, request);
  }

  @Test
  void changePassword_WrongCurrentPassword_PropagatesInvalidPasswordException() {
    ChangePasswordRequestDto request = new ChangePasswordRequestDto("wrong", "newpassword123");
    doThrow(new InvalidPasswordException("currentPassword", "Current password is incorrect"))
        .when(userService).changePassword(currentUser, request);

    assertThrows(InvalidPasswordException.class, () -> userController.changePassword(authentication, request));
  }

  @Test
  void getCurrentUserProfile_ReturnsProfileOfAuthenticatedUser() {
    UserResponseDto expectedDto = new UserResponseDto(1L, "testuser", "test@example.com",
        LocalDateTime.now(), null);
    when(userService.mapToUserProfileDto(currentUser)).thenReturn(expectedDto);

    ResponseEntity<UserResponseDto> response = userController.getCurrentUserProfile(authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedDto, response.getBody());
  }
}
