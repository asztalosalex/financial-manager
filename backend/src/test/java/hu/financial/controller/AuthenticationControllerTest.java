package hu.financial.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;

import hu.financial.dto.user.LoginUserDto;
import hu.financial.dto.user.RegisterUserDto;
import hu.financial.dto.user.UserResponseDto;
import hu.financial.mapper.UserMapper;
import hu.financial.model.User;
import hu.financial.responses.LoginResponse;
import hu.financial.security.SecurityCookieFactory;
import hu.financial.service.AuthenticationService;
import hu.financial.service.JwtService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SecurityCookieFactory securityCookieFactory;

    @InjectMocks
    private AuthenticationController authenticationController;

    private User testUser;
    private RegisterUserDto registerUserDto;
    private LoginUserDto loginUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123", "test@example.com", LocalDateTime.now());
        testUser.setId(1L);

        registerUserDto = new RegisterUserDto("testuser", "password123", "test@example.com");
        loginUserDto = new LoginUserDto("test@example.com", "password123");
    }

    private void stubSuccessfulLogin() {
        when(authenticationService.authenticate(any(LoginUserDto.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
        when(securityCookieFactory.createAuthCookie("token")).thenReturn(
                ResponseCookie.from("authToken", "token").maxAge(Duration.ofSeconds(3600)).path("/").build());
    }

    @Test
    void signup_ShouldReturnUserResponseDto_WhenValidRegistrationData() {
        UserResponseDto responseDto = new UserResponseDto(
                testUser.getId(), testUser.getUsername(), testUser.getEmail(), testUser.getCreatedAt(), null);
        when(authenticationService.signup(any(RegisterUserDto.class))).thenReturn(testUser);
        when(userMapper.mapToDto(testUser)).thenReturn(responseDto);

        ResponseEntity<UserResponseDto> response = authenticationController.signup(registerUserDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        assertEquals(testUser.getUsername(), response.getBody().getUsername());
        assertEquals(testUser.getEmail(), response.getBody().getEmail());
        assertNull(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));

        verify(authenticationService, times(1)).signup(registerUserDto);
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenAuthenticationException() {
        when(authenticationService.authenticate(any(LoginUserDto.class)))
                .thenThrow(new AuthenticationException("Invalid credentials") {
                });

        ResponseEntity<LoginResponse> response = authenticationController.login(loginUserDto);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("invalid_credentials", response.getBody().getMessage());
        assertNull(response.getBody().getExpiresIn());

        verify(authenticationService, times(1)).authenticate(loginUserDto);
        verify(jwtService, never()).generateToken(any(User.class));
        verify(securityCookieFactory, never()).createAuthCookie(any());
    }

    @Test
    void login_ShouldPropagateUnexpectedException_ToGlobalExceptionHandler() {
        when(authenticationService.authenticate(any(LoginUserDto.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThrows(RuntimeException.class, () -> authenticationController.login(loginUserDto));

        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void signup_ShouldCallAuthenticationService_WithCorrectDto() {
        when(authenticationService.signup(any(RegisterUserDto.class))).thenReturn(testUser);

        authenticationController.signup(registerUserDto);

        verify(authenticationService, times(1)).signup(registerUserDto);
    }

    @Test
    void login_ShouldReturnSuccessWithExpiryFromJwtService_AndSetAuthCookie() {
        stubSuccessfulLogin();
        when(jwtService.getExpirationTime()).thenReturn(3600L);

        ResponseEntity<LoginResponse> response = authenticationController.login(loginUserDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", response.getBody().getMessage());
        assertEquals(3600L, response.getBody().getExpiresIn());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).startsWith("authToken=token"));
        verify(authenticationService, times(1)).authenticate(loginUserDto);
    }

    @Test
    void login_ShouldCallJwtService_WithCorrectUser() {
        stubSuccessfulLogin();

        authenticationController.login(loginUserDto);

        verify(jwtService, times(1)).generateToken(testUser);
        verify(securityCookieFactory, times(1)).createAuthCookie("token");
    }

    @Test
    void logout_ShouldReturnNoContent_AndSendExpiredCookie() {
        when(securityCookieFactory.expireAuthCookie()).thenReturn(
                ResponseCookie.from("authToken", "").maxAge(Duration.ZERO).path("/").build());

        ResponseEntity<Void> response = authenticationController.logout();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("Max-Age=0"));
    }
}
