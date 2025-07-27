package hu.financial.controller;

import hu.financial.model.User;
import hu.financial.service.UserService;
import hu.financial.exception.user.UserNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;


    @Test
    void getAllUsers_WithUsers_ReturnsUsers() {
        // Arrange
        List<User> users = Arrays.asList(
            new User("user1", "pass1", "user1@example.com"),
            new User("user2", "pass2", "user2@example.com")
        );
        when(userService.getAllUsers()).thenReturn(users);


        // Act
        ResponseEntity<List<User>> response = userController.getAllUsers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users, response.getBody());
    }

    @Test
    void getAllUsers_EmptyList_ReturnsNoContent() {
        // Arrange
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<User>> response = userController.getAllUsers();

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getUserById_Success() {
        // Arrange
        Long userId = 1L;
        User user = new User("testuser", "password123", "test@example.com");
        user.setId(userId);
        when(userService.getUserById(userId)).thenReturn(user);

        // Act
        ResponseEntity<User> response = userController.getUserById(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody());
    }

    @Test
    void getUserById_UserNotFound_ThrowsException() {
        // Arrange
        Long userId = 999L;
        when(userService.getUserById(userId)).thenThrow(new UserNotFoundException(userId));

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userController.getUserById(userId);
        });
    }

    @Test
    void updateUser_Success() {
        // Arrange
        Long userId = 1L;
        User userDetails = new User("updateduser", "newpassword", "updated@example.com");
        User updatedUser = new User("updateduser", "newpassword", "updated@example.com");
        updatedUser.setId(userId);
        
        when(userService.updateUser(userId, userDetails)).thenReturn(updatedUser);

        // Act
        ResponseEntity<User> response = userController.updateUser(userId, userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedUser, response.getBody());
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        Long userId = 1L;
        doNothing().when(userService).deleteUser(userId);

        // Act
        ResponseEntity<Void> response = userController.deleteUser(userId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService).deleteUser(userId);
    }

    @Test
    void deleteUser_UserNotFound_ThrowsException() {
        // Arrange
        Long userId = 999L;
        doThrow(new UserNotFoundException(userId)).when(userService).deleteUser(userId);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userController.deleteUser(userId);
        });
    }
} 