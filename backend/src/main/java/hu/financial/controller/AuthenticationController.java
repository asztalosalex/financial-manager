package hu.financial.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import hu.financial.responses.LoginResponse;
import hu.financial.dto.user.LoginUserDto;
import hu.financial.dto.user.RegisterUserDto;
import hu.financial.service.AuthenticationService;
import hu.financial.service.JwtService;
import org.springframework.http.ResponseEntity;
import hu.financial.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpHeaders;

@RequestMapping("/api/auth")
@RestController
@Tag(name = "Authentication", description = "Authentication Handler")
public class AuthenticationController {
   
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<User> signup(@RequestBody RegisterUserDto input) {
        User registeredUser = authenticationService.signup(input);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginUserDto input) {
        try {
            System.out.println("Login request received: " + input);
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
            System.out.println("AuthenticationException: " + e.getMessage());
            LoginResponse response = new LoginResponse(null, "invalid_credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            LoginResponse response = new LoginResponse(null, "internal_server_error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
}
