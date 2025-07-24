package hu.financial.service;

import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import hu.financial.dto.LoginUserDto;
import hu.financial.dto.RegisterUserDto;
import hu.financial.model.User;
import hu.financial.repository.UserRepository;
import java.util.List;
import java.util.Collections;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(
        UserRepository userRepository, 
        AuthenticationManager authenticationManager, 
        PasswordEncoder passwordEncoder
    ){
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(RegisterUserDto input) {
        User user = new User();
        user.setUsername(input.getUsername());
        user.setEmail(input.getEmail());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword())
        );
        return userRepository.findByEmail(input.getEmail());
    }
    
    //TODO: get logged in users from security context
    public List<User> getLoggedInUsers() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication == null) {
            return Collections.emptyList();
        }
        return userRepository.findAll();
    }
    


}
