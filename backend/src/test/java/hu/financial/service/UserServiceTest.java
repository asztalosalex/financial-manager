package hu.financial.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hu.financial.dto.user.ChangePasswordRequestDto;
import hu.financial.exception.user.InvalidPasswordException;
import hu.financial.exception.user.UserNotFoundException;
import hu.financial.model.User;
import hu.financial.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import hu.financial.dto.user.UpdateProfileDto;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", "test@example.com");
        testUser.setId(1L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
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
        UpdateProfileDto updateProfileDto = new UpdateProfileDto("updateduser", "updated@example.com");
        User updatedUser = new User("updateduser", "newpassword", "updated@example.com");
        updatedUser.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        User result = userService.updateUser(userId, updateProfileDto);

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
        UpdateProfileDto updateProfileDto = new UpdateProfileDto("updateduser", "updated@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.updateUser(userId, updateProfileDto);
        });
        verify(userRepository, times(1)).findById(userId);
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
    void changePassword_CorrectCurrentPassword_StoresEncodedNewPassword() {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto("current123", "newpassword123");
        when(passwordEncoder.matches("current123", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded-new-password");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        userService.changePassword(testUser, request);

        assertEquals("encoded-new-password", testUser.getPassword());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void changePassword_WrongCurrentPassword_ThrowsInvalidPasswordException() {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto("wrong", "newpassword123");
        when(passwordEncoder.matches("wrong", testUser.getPassword())).thenReturn(false);

        InvalidPasswordException exception = assertThrows(InvalidPasswordException.class,
                () -> userService.changePassword(testUser, request));

        assertEquals("currentPassword", exception.getField());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void getCurrentUser_ReturnsPrincipalFromSecurityContext_WithoutQueryingRepository() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null));

        User result = userService.getCurrentUser();

        assertSame(testUser, result);
        verify(userRepository, never()).findByUsername(any());
    }
}