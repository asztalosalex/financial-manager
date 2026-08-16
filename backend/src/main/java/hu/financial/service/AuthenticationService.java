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
        if (userRepository.findByEmail(input.email()) != null) {
            throw new DuplicateUserException("email", input.email());
        }
        if (userRepository.findByUsername(input.username()) != null) {
            throw new DuplicateUserException("username", input.username());
        }

        User user = new User();
        user.setUsername(input.username());
        user.setEmail(input.email());
        user.setPassword(passwordEncoder.encode(input.password()));
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
            new UsernamePasswordAuthenticationToken(input.email(), input.password())
        );

        User user = userRepository.findByEmail(input.email());
        if (user == null) {
            throw new UserNotFoundException("User not found with email: " + input.email());
        }
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }
}
