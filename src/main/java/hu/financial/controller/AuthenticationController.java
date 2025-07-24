package hu.financial.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import hu.financial.dto.LoginUserDto;
import hu.financial.dto.RegisterUserDto;
import hu.financial.service.AuthenticationService;
import hu.financial.service.JwtService;
import org.springframework.http.ResponseEntity;
import hu.financial.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import hu.financial.exception.user.UserNotFoundException;
import org.springframework.security.core.AuthenticationException;

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
    public ResponseEntity<String> login(@RequestBody LoginUserDto input) {
        try {
            User user = authenticationService.authenticate(input);
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(token);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    
}
