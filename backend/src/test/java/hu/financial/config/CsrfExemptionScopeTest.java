package hu.financial.config;

import tools.jackson.databind.ObjectMapper;
import hu.financial.controller.AuthenticationController;
import hu.financial.controller.CategoryController;
import hu.financial.controller.UserController;
import hu.financial.dto.category.CreateCategoryDto;
import hu.financial.dto.user.LoginUserDto;
import hu.financial.dto.user.RegisterUserDto;
import hu.financial.dto.user.UserResponseDto;
import hu.financial.filter.JwtAuthenticationFilter;
import hu.financial.mapper.UserMapper;
import hu.financial.model.User;
import hu.financial.security.CookieProperties;
import hu.financial.security.CsrfCookieFilter;
import hu.financial.security.RestAccessDeniedHandler;
import hu.financial.security.SecurityCookieFactory;
import hu.financial.service.AuthenticationService;
import hu.financial.service.CategoryService;
import hu.financial.service.JwtService;
import hu.financial.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { AuthenticationController.class, CategoryController.class, UserController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CookieProperties.class, RestAccessDeniedHandler.class,
        SecurityCookieFactory.class, CsrfCookieFilter.class, JwtService.class, FilterRegistrationConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.jwt.expiration-time=3600",
        "security.cookie.secure=false",
        "security.cookie.same-site=Lax"
})
class CsrfExemptionScopeTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserMapper userMapper;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User("testuser", "encoded-password", "test@example.com", LocalDateTime.now());
        currentUser.setId(1L);

        lenient().when(userService.loadUserByUsername("testuser")).thenReturn(currentUser);
        lenient().when(userService.getCurrentUser()).thenReturn(currentUser);
        lenient().when(authenticationService.authenticate(any(LoginUserDto.class))).thenReturn(currentUser);
        lenient().when(authenticationService.signup(any(RegisterUserDto.class))).thenReturn(currentUser);
        lenient().when(userMapper.mapToDto(any(User.class))).thenReturn(
                new UserResponseDto(1L, "testuser", "test@example.com", LocalDateTime.now(), null));
    }

    private Cookie authCookie() {
        return new Cookie("authToken", jwtService.generateToken(currentUser));
    }

    private String categoryBody() throws Exception {
        return objectMapper.writeValueAsString(new CreateCategoryDto("Groceries", "Weekly food shopping"));
    }

    @ParameterizedTest(name = "[{index}] {0} {1} is not CSRF-exempt")
    @CsvSource({
            "POST,   /api/categories",
            "PUT,    /api/categories/1",
            "DELETE, /api/categories/1",
            "POST,   /api/users/change-password",
            "PUT,    /api/users/1",
            "DELETE, /api/users/1",
            "PUT,    /api/auth/login",
            "DELETE, /api/auth/login",
            "PUT,    /api/auth/signup",
            "DELETE, /api/auth/logout",
            "POST,   /api/auth/loginX",
            "POST,   /api/auth/login/extra",
            "POST,   /api/auth/signup/extra",
            "POST,   /api/auth/logout/extra",
            "POST,   /api/auth",
            "POST,   /api/auth/refresh"
    })
    void mutatingRequestWithoutCsrfToken_IsRejected(String method, String path) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .request(HttpMethod.valueOf(method), path)
                .cookie(authCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryBody());

        mockMvc.perform(request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void login_IsCsrfExempt_BecauseItIsPreAuth() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginUserDto("test@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    void signup_IsCsrfExempt_BecauseItIsPreAuth() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterUserDto("testuser", "password123", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void logout_IsCsrfExempt_BecauseItIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void theSameCategoryPost_SucceedsOnceTheCsrfTokenIsPresent() throws Exception {
        Cookie csrf = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/count"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(csrf, "no XSRF-TOKEN cookie was issued");

        mockMvc.perform(post("/api/categories")
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryBody()))
                .andExpect(status().isCreated());
    }
}
