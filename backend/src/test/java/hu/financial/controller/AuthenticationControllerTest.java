package hu.financial.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;

import hu.financial.dto.user.LoginUserDto;
import hu.financial.dto.user.RegisterUserDto;
import hu.financial.model.User;
import hu.financial.responses.LoginResponse;
import hu.financial.service.AuthenticationService;
import hu.financial.service.JwtService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;

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
        testUser = new User("testuser", "password123", "test@example.com", LocalDateTime.now());
        testUser.setId(1L);
        
        registerUserDto = new RegisterUserDto("testuser", "password123", "test@example.com", LocalDateTime.now());
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
    void login_ShouldReturnUnauthorized_WhenAuthenticationException() {
        // Arrange
        when(authenticationService.authenticate(any(LoginUserDto.class)))
            .thenThrow(new AuthenticationException("Invalid credentials") {});

        // Act
        ResponseEntity<LoginResponse> response = authenticationController.login(loginUserDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("invalid_credentials", response.getBody().getStatus());
        
        verify(authenticationService, times(1)).authenticate(loginUserDto);
        verify(jwtService, never()).generateToken(any(User.class));
    }


    @Test
    void login_ShouldReturnInternalServerError_WhenUnexpectedException() {
        // Arrange
        when(authenticationService.authenticate(any(LoginUserDto.class)))
            .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<LoginResponse> response = authenticationController.login(loginUserDto);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("internal_server_error", response.getBody().getStatus());
        
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
