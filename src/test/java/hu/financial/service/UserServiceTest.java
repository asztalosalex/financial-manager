package hu.financial.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hu.financial.exception.user.DuplicateUserException;
import hu.financial.exception.user.UserNotFoundException;
import hu.financial.model.User;
import hu.financial.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", "test@example.com");
        testUser.setId(1L);
        
        anotherUser = new User("anotheruser", "password456", "another@example.com");
        anotherUser.setId(2L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_WithUsers_ReturnsAllUsers() {
        // Arrange
        List<User> expectedUsers = Arrays.asList(testUser, anotherUser);
        when(userRepository.findAll()).thenReturn(expectedUsers);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedUsers, result);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_EmptyList_ThrowsUserNotFoundException() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.getAllUsers();
        });
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserById_ExistingUser_ReturnsUser() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserById_NonExistingUser_ThrowsUserNotFoundException() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(userId);
        });
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void updateUser_ValidUser_ReturnsUpdatedUser() {
        // Arrange
        Long userId = 1L;
        User updatedUser = new User("updateduser", "newpassword", "updated@example.com");
        updatedUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(updatedUser.getUsername())).thenReturn(null);
        when(userRepository.findByEmail(updatedUser.getEmail())).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        User result = userService.updateUser(userId, updatedUser);

        // Assert
        assertNotNull(result);
        assertEquals(updatedUser.getUsername(), result.getUsername());
        assertEquals(updatedUser.getEmail(), result.getEmail());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_NonExistingUser_ThrowsUserNotFoundException() {
        // Arrange
        Long userId = 999L;
        User updatedUser = new User("updateduser", "newpassword", "updated@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.updateUser(userId, updatedUser);
        });
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_DuplicateUsername_ThrowsDuplicateUserException() {
        // Arrange
        Long userId = 1L;
        User updatedUser = new User("anotheruser", "newpassword", "updated@example.com");
        updatedUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(updatedUser.getUsername())).thenReturn(anotherUser);

        // Act & Assert
        assertThrows(DuplicateUserException.class, () -> {
            userService.updateUser(userId, updatedUser);
        });
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).findByUsername(updatedUser.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_DuplicateEmail_ThrowsDuplicateUserException() {
        // Arrange
        Long userId = 1L;
        User updatedUser = new User("updateduser", "newpassword", "another@example.com");
        updatedUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(updatedUser.getUsername())).thenReturn(null);
        when(userRepository.findByEmail(updatedUser.getEmail())).thenReturn(anotherUser);

        // Act & Assert
        assertThrows(DuplicateUserException.class, () -> {
            userService.updateUser(userId, updatedUser);
        });
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).findByUsername(updatedUser.getUsername());
        verify(userRepository, times(1)).findByEmail(updatedUser.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_ExistingUser_DeletesSuccessfully() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).deleteById(userId);

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void deleteUser_NonExistingUser_ThrowsUserNotFoundException() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(userId);
        });
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void isUserLoggedIn_UserExists_ReturnsTrue() {
        // Arrange
        String username = "testuser";

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(testUser);

        // Act
        boolean result = userService.isUserLoggedIn();

        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void isUserLoggedIn_UserNotExists_ReturnsFalse() {
        // Arrange
        String username = "testuser";

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(username)).thenReturn(null);

        // Act
        boolean result = userService.isUserLoggedIn();

        // Assert
        assertFalse(result);
        
        verify(userRepository, times(1)).findByUsername(username);
    }
}