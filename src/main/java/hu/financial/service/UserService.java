package hu.financial.service;

import hu.financial.model.User;
import hu.financial.repository.UserRepository;
import hu.financial.exception.user.DuplicateUserException;
import hu.financial.exception.user.UserNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
  
    
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new UserNotFoundException("No users found");
        }
        return users;
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
    
    public User updateUser(Long id, User userDetails) {
        User existingUser = getUserById(id);
        validateUserForUpdate(existingUser, userDetails);
        
        // Update user fields
        existingUser.setUsername(userDetails.getUsername());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPassword(userDetails.getPassword());
        
        return userRepository.save(existingUser);
    }
    
    public void deleteUser(Long id) {
        if (userRepository.findById(id).isEmpty()) {
            throw new UserNotFoundException(id);
        }
        else {
            userRepository.deleteById(id);
        }
    }
    
    private void validateUserForUpdate(User existingUser, User userDetails) {
        // Check for duplicate username if username is being changed
        if (!existingUser.getUsername().equals(userDetails.getUsername())) {
            User userWithSameUsername = userRepository.findByUsername(userDetails.getUsername());
            if (userWithSameUsername != null) {
                throw new DuplicateUserException("username", userDetails.getUsername());
            }
        }
        
        // Check for duplicate email if email is being changed
        if (StringUtils.hasText(userDetails.getEmail()) && 
            !userDetails.getEmail().equals(existingUser.getEmail())) {
            User userWithSameEmail = userRepository.findByEmail(userDetails.getEmail());
            if (userWithSameEmail != null) {
                throw new DuplicateUserException("email", userDetails.getEmail());
            }
        }
    }

    public boolean isUserLoggedIn() {
        return userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName()) != null;
    }
} 