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
import hu.financial.service.AuthenticationService;
import hu.financial.service.JwtService;
import org.springframework.http.ResponseEntity;
import hu.financial.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService,
            UserMapper userMapper) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
        this.userMapper = userMapper;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@Valid @RequestBody RegisterUserDto input) {
        User registeredUser = authenticationService.signup(input);
        return ResponseEntity.ok(userMapper.mapToDto(registeredUser));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginUserDto input) {
        try {
            User user = authenticationService.authenticate(input);
            String token = jwtService.generateToken(user);
            Long expiresIn = jwtService.getExpirationTime();
            LoginResponse response = new LoginResponse(expiresIn, "success");

            ResponseCookie cookie = ResponseCookie.from("authToken", token)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .maxAge(Duration.ofMinutes(120))
            .build();

            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response);

        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for email: {}", input.getEmail());
            LoginResponse response = new LoginResponse(null, "invalid_credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            LoginResponse response = new LoginResponse(null, "internal_server_error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
