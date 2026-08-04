package hu.financial.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hu.financial.filter.JwtAuthenticationFilter;
import hu.financial.security.CsrfCookieFilter;

@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CsrfCookieFilter> csrfCookieFilterRegistration(
            CsrfCookieFilter csrfCookieFilter) {
        FilterRegistrationBean<CsrfCookieFilter> registration = new FilterRegistrationBean<>(csrfCookieFilter);
        registration.setEnabled(false);
        return registration;
    }
}
