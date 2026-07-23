package hu.financial.service;

import hu.financial.dto.user.UpdateProfileDto;
import hu.financial.dto.user.UserResponseDto;
import hu.financial.dto.user.GetUserByIdDto;
import hu.financial.mapper.UserMapper;
import hu.financial.model.User;
import hu.financial.repository.UserRepository;
import hu.financial.exception.user.DuplicateUserException;
import hu.financial.exception.user.UserNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class UserService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;
  
    
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new UserNotFoundException("No users found");
        }
        return users;
    }

    public long countUsers(){
        return userRepository.count();
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
    
    public User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    public User getUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UserNotFoundException("User not found with username: " + username);
        }
        return user;
    }
    
    public User updateUser(Long id, UpdateProfileDto updateProfileDto) {
        User existingUser = getUserById(id);
    
        User userDetails = new User();
        userDetails.setUsername(updateProfileDto.getUsername());
        userDetails.setEmail(updateProfileDto.getEmail());
        
        validateUserForUpdate(existingUser, userDetails);
        
        if (StringUtils.hasText(updateProfileDto.getUsername())) {
            existingUser.setUsername(updateProfileDto.getUsername());
        }
        if (StringUtils.hasText(updateProfileDto.getEmail())) {
            existingUser.setEmail(updateProfileDto.getEmail());
        }
        
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
        if (!Objects.equals(existingUser.getUsername(), userDetails.getUsername())) {
            User userWithSameUsername = userRepository.findByUsername(userDetails.getUsername());
            if (userWithSameUsername != null && !userWithSameUsername.getId().equals(existingUser.getId())) {
                throw new DuplicateUserException("username", userDetails.getUsername());
            }
        }

        // Check for duplicate email if email is being changed
        if (StringUtils.hasText(userDetails.getEmail()) &&
                !Objects.equals(existingUser.getEmail(), userDetails.getEmail())) {
            User userWithSameEmail = userRepository.findByEmail(userDetails.getEmail());
            if (userWithSameEmail != null && !userWithSameEmail.getId().equals(existingUser.getId())) {
                throw new DuplicateUserException("email", userDetails.getEmail());
            }
        }
    }

    public boolean isUserLoggedIn() {
        return userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName()) != null;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            user = userRepository.findByEmail(username);
        }
        
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username/email: " + username);
        }
        return user;
    }
    
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    public void changePassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    public User getCurrentUser() {
        return userRepository.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    public GetUserByIdDto getUserByIdDto(Long id) {
        User user = getUserById(id);
        return userMapper.toGetUserByIdDto(user);
    }

    public UpdateProfileDto updateProfileDto(User user){

        return userMapper.mapToUpdateProfileDto(user);
    }

    public UserResponseDto mapToUserProfileDto(User user){
        return userMapper.mapToDto(user);
    }
} 
