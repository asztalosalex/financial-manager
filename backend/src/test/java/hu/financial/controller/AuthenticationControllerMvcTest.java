package hu.financial.controller;

import tools.jackson.databind.ObjectMapper;
import hu.financial.config.FilterRegistrationConfig;
import hu.financial.config.SecurityConfig;
import hu.financial.dto.user.LoginUserDto;
import hu.financial.dto.user.RegisterUserDto;
import hu.financial.dto.user.UserResponseDto;
import hu.financial.exception.user.DuplicateUserException;
import hu.financial.filter.JwtAuthenticationFilter;
import hu.financial.mapper.UserMapper;
import hu.financial.model.User;
import hu.financial.security.SecurityCookieFactory;
import hu.financial.security.CookieProperties;
import hu.financial.security.CsrfCookieFilter;
import hu.financial.security.RestAccessDeniedHandler;
import hu.financial.service.AuthenticationService;
import hu.financial.service.JwtService;
import hu.financial.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CookieProperties.class, RestAccessDeniedHandler.class,
        SecurityCookieFactory.class, CsrfCookieFilter.class, JwtService.class, FilterRegistrationConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.jwt.expiration-time=3600",
        "security.cookie.secure=false",
        "security.cookie.same-site=Lax"
})
class AuthenticationControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "encoded-password", "test@example.com", LocalDateTime.now());
        testUser.setId(1L);
    }

    private String authCookieHeader(MvcResult result) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("authToken="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no authToken Set-Cookie header was sent"));
    }

    @Test
    void login_ValidCredentials_Returns200AndSetsAuthCookieWithContractAttributes() throws Exception {
        when(authenticationService.authenticate(any(LoginUserDto.class))).thenReturn(testUser);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginUserDto("test@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn();

        String setCookie = authCookieHeader(result);
        assertThat(setCookie, containsString("HttpOnly"));
        assertThat(setCookie, containsString("Path=/"));
        assertThat(setCookie, containsString("SameSite=Lax"));
        assertThat(setCookie, containsString("Max-Age=3600"));
        assertThat(setCookie, not(containsString("Secure")));
    }

    @Test
    void login_InvalidCredentials_Returns401WithLoginResponse() throws Exception {
        when(authenticationService.authenticate(any(LoginUserDto.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginUserDto("test@example.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("invalid_credentials"))
                .andExpect(jsonPath("$.expiresIn").doesNotExist());
    }

    @Test
    void login_InvalidBody_Returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginUserDto("", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void signup_ValidBody_Returns200AndSetsNoAuthCookie() throws Exception {
        when(authenticationService.signup(any(RegisterUserDto.class))).thenReturn(testUser);
        when(userMapper.mapToDto(testUser)).thenReturn(
                new UserResponseDto(1L, "testuser", "test@example.com", LocalDateTime.now(), null));

        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterUserDto("testuser", "password123", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andReturn();

        assertTrue(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .noneMatch(value -> value.startsWith("authToken=")),
                "signup must not authenticate the user");
    }

    @Test
    void signup_InvalidEmail_Returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterUserDto("testuser", "password123", "not-an-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void signup_DuplicateEmail_Returns409() throws Exception {
        when(authenticationService.signup(any(RegisterUserDto.class)))
                .thenThrow(new DuplicateUserException("email", "test@example.com"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterUserDto("testuser", "password123", "test@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void logout_WithoutCookieAndWithoutCsrfToken_Returns204AndClearsAuthCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn();

        String setCookie = authCookieHeader(result);
        assertThat(setCookie, containsString("Max-Age=0"));
        assertThat(setCookie, containsString("Path=/"));
        assertThat(setCookie, containsString("SameSite=Lax"));
        assertThat(setCookie, containsString("HttpOnly"));
    }

    @Test
    void logout_WithExistingCookie_Returns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("authToken", "whatever")))
                .andExpect(status().isNoContent());
    }
}
