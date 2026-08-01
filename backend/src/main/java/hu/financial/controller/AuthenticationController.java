package hu.financial.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import hu.financial.responses.LoginResponse;
import hu.financial.dto.user.LoginUserDto;
import hu.financial.dto.user.RegisterUserDto;
import hu.financial.dto.user.UserResponseDto;
import hu.financial.mapper.UserMapper;
import hu.financial.security.SecurityCookieFactory;
import hu.financial.service.AuthenticationService;
import hu.financial.service.JwtService;
import org.springframework.http.ResponseEntity;
import hu.financial.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpHeaders;

@RequestMapping("/api/auth")
@RestController
@Tag(name = "Authentication", description = "Authentication Handler")
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private final UserMapper userMapper;
    private final SecurityCookieFactory securityCookieFactory;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService,
            UserMapper userMapper, SecurityCookieFactory securityCookieFactory) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
        this.userMapper = userMapper;
        this.securityCookieFactory = securityCookieFactory;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@Valid @RequestBody RegisterUserDto input) {
        User registeredUser = authenticationService.signup(input);
        return ResponseEntity.ok(userMapper.mapToDto(registeredUser));
    }

    @Operation(summary = "Log in and receive the authentication cookie")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginUserDto input) {
        try {
            User user = authenticationService.authenticate(input);
            String token = jwtService.generateToken(user);
            LoginResponse response = new LoginResponse(jwtService.getExpirationTime(), "success");

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, securityCookieFactory.createAuthCookie(token).toString())
                    .body(response);
        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for email: {}", input.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, "invalid_credentials"));
        }
    }

    @Operation(summary = "Log out and clear the authentication cookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, securityCookieFactory.expireAuthCookie().toString())
                .build();
    }
}
