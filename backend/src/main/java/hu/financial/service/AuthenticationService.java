package hu.financial.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import hu.financial.dto.user.LoginUserDto;
import hu.financial.dto.user.RegisterUserDto;
import hu.financial.model.User;
import hu.financial.repository.UserRepository;
import hu.financial.exception.user.DuplicateUserException;
import hu.financial.exception.user.UserNotFoundException;
import java.time.LocalDateTime;

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

    @Transactional
    public User signup(RegisterUserDto input) {
        if (userRepository.findByEmail(input.getEmail()) != null) {
            throw new DuplicateUserException("email", input.getEmail());
        }
        if (userRepository.findByUsername(input.getUsername()) != null) {
            throw new DuplicateUserException("username", input.getUsername());
        }

        User user = new User();
        user.setUsername(input.getUsername());
        user.setEmail(input.getEmail());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUserException("Account with this email or username already exists");
        }
    }

    @Transactional
    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword())
        );

        User user = userRepository.findByEmail(input.getEmail());
        if (user == null) {
            throw new UserNotFoundException("User not found with email: " + input.getEmail());
        }
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }
}
