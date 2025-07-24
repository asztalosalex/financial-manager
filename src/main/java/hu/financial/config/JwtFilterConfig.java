package hu.financial.config;

import hu.financial.filter.JwtAuthenticationFilter;
import hu.financial.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import hu.financial.model.User;
import hu.financial.repository.UserRepository;

@Configuration
public class JwtFilterConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
                User user = userRepository.findByUsername(usernameOrEmail);
                if (user == null) {
                    user = userRepository.findByEmail(usernameOrEmail);
                }
                if (user == null) {
                    throw new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
                }
                return user;
            }
        };
    }
}
