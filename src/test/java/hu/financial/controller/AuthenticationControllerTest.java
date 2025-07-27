package hu.financial.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;

import hu.financial.dto.LoginUserDto;
import hu.financial.dto.RegisterUserDto;
import hu.financial.exception.user.UserNotFoundException;
import hu.financial.model.User;
import hu.financial.service.AuthenticationService;
import hu.financial.service.JwtService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private User testUser;
    private RegisterUserDto registerUserDto;
    private LoginUserDto loginUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", "test@example.com");
        testUser.setId(1L);
        
        registerUserDto = new RegisterUserDto("testuser", "password123", "test@example.com");
        loginUserDto = new LoginUserDto("test@example.com", "password123");
    }

    @Test
    void signup_ShouldReturnUser_WhenValidRegistrationData() {
        // Arrange
        when(authenticationService.signup(any(RegisterUserDto.class))).thenReturn(testUser);

        // Act
        ResponseEntity<User> response = authenticationController.signup(registerUserDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUser.getUsername(), response.getBody().getUsername());
        assertEquals(testUser.getEmail(), response.getBody().getEmail());
        
        verify(authenticationService, times(1)).signup(registerUserDto);
    }

    @Test
    void login_ShouldReturnToken_WhenValidCredentials() {
        // Arrange
        String expectedToken = "jwt-token-123";
        when(authenticationService.authenticate(any(LoginUserDto.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn(expectedToken);

        // Act
        ResponseEntity<String> response = authenticationController.login(loginUserDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedToken, response.getBody());
        
        verify(authenticationService, times(1)).authenticate(loginUserDto);
        verify(jwtService, times(1)).generateToken(testUser);
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenAuthenticationException() {
        // Arrange
        when(authenticationService.authenticate(any(LoginUserDto.class)))
            .thenThrow(new AuthenticationException("Invalid credentials") {});

        // Act
        ResponseEntity<String> response = authenticationController.login(loginUserDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
        
        verify(authenticationService, times(1)).authenticate(loginUserDto);
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenUserNotFoundException() {
        // Arrange
        when(authenticationService.authenticate(any(LoginUserDto.class)))
            .thenThrow(new UserNotFoundException("User not found"));

        // Act
        ResponseEntity<String> response = authenticationController.login(loginUserDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
        
        verify(authenticationService, times(1)).authenticate(loginUserDto);
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void login_ShouldReturnInternalServerError_WhenUnexpectedException() {
        // Arrange
        when(authenticationService.authenticate(any(LoginUserDto.class)))
            .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<String> response = authenticationController.login(loginUserDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody());
        
        verify(authenticationService, times(1)).authenticate(loginUserDto);
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void signup_ShouldCallAuthenticationService_WithCorrectDto() {
        // Arrange
        when(authenticationService.signup(any(RegisterUserDto.class))).thenReturn(testUser);

        // Act
        authenticationController.signup(registerUserDto);

        // Assert
        verify(authenticationService, times(1)).signup(registerUserDto);
    }

    @Test
    void login_ShouldCallAuthenticationService_WithCorrectDto() {
        // Arrange
        when(authenticationService.authenticate(any(LoginUserDto.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("token");

        // Act
        authenticationController.login(loginUserDto);

        // Assert
        verify(authenticationService, times(1)).authenticate(loginUserDto);
    }

    @Test
    void login_ShouldCallJwtService_WithCorrectUser() {
        // Arrange
        when(authenticationService.authenticate(any(LoginUserDto.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("token");

        // Act
        authenticationController.login(loginUserDto);

        // Assert
        verify(jwtService, times(1)).generateToken(testUser);
    }
}
