package hu.financial.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import hu.financial.model.User;
import hu.financial.dto.user.RegisterUserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.AuthenticationException;
import hu.financial.dto.user.LoginUserDto;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.security.crypto.password.PasswordEncoder;
import hu.financial.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

  @InjectMocks
  private AuthenticationService authenticationService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private PasswordEncoder passwordEncoder;

  private User testUser;
  private RegisterUserDto registerUserDto;
  private LoginUserDto loginUserDto;

  @BeforeEach
  void setUp() {
    testUser = new User("testuser", "password123", "test@example.com", LocalDateTime.now());
    testUser.setId(1L);
    registerUserDto = new RegisterUserDto("testuser", "password123", "test@example.com", LocalDateTime.now());
    loginUserDto = new LoginUserDto("test@example.com", "password123");
  }

  @Test
  void signup_ShouldReturnUser_WhenValidRegistrationData() {
    // Arrange
    when(passwordEncoder.encode(registerUserDto.getPassword())).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    // Act
    User result = authenticationService.signup(registerUserDto);

    // Assert
    assertNotNull(result);
    assertEquals(testUser, result);
    verify(passwordEncoder).encode(registerUserDto.getPassword());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void signup_ShouldThrowException_WhenInvalidRegistrationData() {
    // Arrange
    when(passwordEncoder.encode(registerUserDto.getPassword())).thenThrow(new RuntimeException("Invalid password"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> authenticationService.signup(registerUserDto));
    verify(passwordEncoder).encode(registerUserDto.getPassword());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void authenticate_ShouldReturnUser_WhenValidCredentials() {

    Authentication authentication = mock(Authentication.class);
    // Arrange
    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(userRepository.findByEmail(loginUserDto.getEmail())).thenReturn(testUser);
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    // Act
    User result = authenticationService.authenticate(loginUserDto);

    // Assert
    assertNotNull(result);
    assertEquals(testUser, result);
    verify(authenticationManager).authenticate(any());
    verify(userRepository).findByEmail(loginUserDto.getEmail());
    verify(userRepository).save(testUser);
  }

  @Test
  void authenticate_ShouldThrowException_WhenInvalidCredentials() {
    when(authenticationManager.authenticate(any())).thenThrow(new AuthenticationException("Authentication failed") {
    });

    // Act & Assert
    assertThrows(AuthenticationException.class, () -> {
      authenticationService.authenticate(loginUserDto);
    });
    verify(authenticationManager).authenticate(any());
  }

  @Test
  void authenticate_ShouldThrowException_WhenUserNotFound() {
    // Arrange
    when(authenticationManager.authenticate(any())).thenReturn(null);
    when(userRepository.findByEmail(loginUserDto.getEmail())).thenReturn(null);

    // Act & Assert
    assertThrows(NullPointerException.class, () -> {
      authenticationService.authenticate(loginUserDto);
    });
    verify(authenticationManager).authenticate(any());
    verify(userRepository).findByEmail(loginUserDto.getEmail());
  }

  @Test
  void authenticate_ShouldThrowException_WhenUserNotAuthenticated() {
    // Arrange
    when(authenticationManager.authenticate(any())).thenThrow(new AuthenticationException("Authentication failed") {
    });

    // Act & Assert
    assertThrows(AuthenticationException.class, () -> {
      authenticationService.authenticate(loginUserDto);
    });
    verify(authenticationManager).authenticate(any());
    verify(userRepository, never()).findByEmail(any());
  }
}
