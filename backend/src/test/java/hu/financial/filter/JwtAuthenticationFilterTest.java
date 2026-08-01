package hu.financial.filter;

import hu.financial.model.User;
import hu.financial.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private User user;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        user = new User("testuser", "encoded-password", "test@example.com");
        user.setId(1L);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_ShouldAuthenticate_WhenAuthTokenCookieIsValid() throws Exception {
        request.setCookies(new Cookie("authToken", "valid"));
        when(jwtService.extractUsername("valid")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(user);
        when(jwtService.isTokenValid(eq("valid"), any())).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldIgnoreAuthorizationHeader_BecauseBearerTokensAreNoLongerAccepted() throws Exception {
        request.addHeader("Authorization", "Bearer valid");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldLeaveContextEmpty_WhenTokenIsInvalid() throws Exception {
        request.setCookies(new Cookie("authToken", "invalid"));
        when(jwtService.extractUsername("invalid")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(user);
        when(jwtService.isTokenValid(eq("invalid"), any())).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldSwallowParsingFailure_ButStillContinueTheChain() throws Exception {
        request.setCookies(new Cookie("authToken", "broken"));
        when(jwtService.extractUsername("broken")).thenThrow(new IllegalStateException("malformed token"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldDoNothing_WhenNoCookiesArePresent() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }
}
