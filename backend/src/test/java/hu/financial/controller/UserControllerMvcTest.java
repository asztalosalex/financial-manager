package hu.financial.controller;

import tools.jackson.databind.ObjectMapper;
import hu.financial.config.FilterRegistrationConfig;
import hu.financial.config.SecurityConfig;
import hu.financial.dto.user.ChangePasswordRequestDto;
import hu.financial.dto.user.UpdateProfileDto;
import hu.financial.dto.user.UserResponseDto;
import hu.financial.exception.user.DuplicateUserException;
import hu.financial.exception.user.InvalidPasswordException;
import hu.financial.filter.JwtAuthenticationFilter;
import hu.financial.model.User;
import hu.financial.security.CookieProperties;
import hu.financial.security.CsrfCookieFilter;
import hu.financial.security.RestAccessDeniedHandler;
import hu.financial.security.SecurityCookieFactory;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CookieProperties.class, RestAccessDeniedHandler.class,
        SecurityCookieFactory.class, CsrfCookieFilter.class, JwtService.class, FilterRegistrationConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.jwt.expiration-time=3600",
        "security.cookie.secure=false",
        "security.cookie.same-site=Lax"
})
class UserControllerMvcTest {

    private static final Long OWN_ID = 1L;
    private static final Long FOREIGN_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User("testuser", "encoded-password", "test@example.com");
        currentUser.setId(OWN_ID);

        lenient().when(userService.loadUserByUsername("testuser")).thenReturn(currentUser);
        lenient().when(userService.mapToUserProfileDto(any(User.class))).thenReturn(
                new UserResponseDto(OWN_ID, "testuser", "test@example.com", LocalDateTime.now(), null));
    }

    private Cookie authCookie() {
        return new Cookie("authToken", jwtService.generateToken(currentUser));
    }

    private Cookie csrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/profile").cookie(authCookie()))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie, "XSRF-TOKEN cookie must be issued so the SPA can echo it back");
        return cookie;
    }

    private Cookie anonymousCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie, "an anonymous request must also receive an XSRF-TOKEN cookie");
        return cookie;
    }

    @Test
    void getProfile_WithoutAuthCookie_Returns401AndEmptyBody() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    @Test
    void getProfile_Returns401_ButStillIssuesAReadableCsrfCookie_SoALoggedOutVisitorCanPost() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Cookie csrf = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrf, "the mount-time profile probe is where a cold browser gets its CSRF token");
        org.junit.jupiter.api.Assertions.assertFalse(csrf.isHttpOnly(), "the SPA has to read XSRF-TOKEN from JS");
        org.junit.jupiter.api.Assertions.assertNotNull(csrf.getValue());
        org.junit.jupiter.api.Assertions.assertFalse(csrf.getValue().isBlank());
    }

    @Test
    void getProfile_WithMalformedToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/profile").cookie(new Cookie("authToken", "not.a.jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_WithAuthCookie_Returns200() throws Exception {
        mockMvc.perform(get("/api/users/profile").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getProfile_WithBearerHeaderOnly_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + jwtService.generateToken(currentUser)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserById_ForeignId_Returns404WithErrorResponseBody() throws Exception {
        mockMvc.perform(get("/api/users/{id}", FOREIGN_ID).cookie(authCookie()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());

        verify(userService, never()).getUserByIdDto(any());
    }

    @Test
    void updateUser_ForeignId_Returns404() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/users/{id}", FOREIGN_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileDto("other", "other@example.com"))))
                .andExpect(status().isNotFound());

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void updateUser_InvalidBody_Returns400WithFieldErrors() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/users/{id}", OWN_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileDto("", "not-an-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void updateUser_MalformedJson_Returns400() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/users/{id}", OWN_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateUser_TakenUsername_Returns409() throws Exception {
        Cookie csrf = csrfCookie();
        when(userService.updateUser(eq(OWN_ID), any(UpdateProfileDto.class)))
                .thenThrow(new DuplicateUserException("username", "taken"));

        mockMvc.perform(put("/api/users/{id}", OWN_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileDto("taken", "taken@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateUser_OwnIdWithValidBody_Returns200() throws Exception {
        Cookie csrf = csrfCookie();
        when(userService.updateUser(eq(OWN_ID), any(UpdateProfileDto.class))).thenReturn(currentUser);

        mockMvc.perform(put("/api/users/{id}", OWN_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileDto("testuser", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void updateUser_WithoutAuthCookie_Returns401AndEmptyBody() throws Exception {
        Cookie csrf = anonymousCsrfCookie();

        mockMvc.perform(put("/api/users/{id}", OWN_ID)
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileDto("testuser", "test@example.com"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void updateUser_WithMalformedAuthCookie_Returns401() throws Exception {
        Cookie csrf = anonymousCsrfCookie();

        mockMvc.perform(put("/api/users/{id}", OWN_ID)
                .cookie(new Cookie("authToken", "not.a.jwt"), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileDto("testuser", "test@example.com"))))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void deleteUser_WithoutAuthCookie_Returns401AndEmptyBody() throws Exception {
        Cookie csrf = anonymousCsrfCookie();

        mockMvc.perform(delete("/api/users/{id}", OWN_ID)
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verify(userService, never()).deleteUser(any());
    }

    @Test
    void deleteUser_WithMalformedAuthCookie_Returns401() throws Exception {
        Cookie csrf = anonymousCsrfCookie();

        mockMvc.perform(delete("/api/users/{id}", OWN_ID)
                .cookie(new Cookie("authToken", "not.a.jwt"), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).deleteUser(any());
    }

    @Test
    void deleteUser_ForeignId_Returns404() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(delete("/api/users/{id}", FOREIGN_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound());

        verify(userService, never()).deleteUser(any());
    }

    @Test
    void deleteUser_OwnId_Returns204() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(delete("/api/users/{id}", OWN_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(OWN_ID);
    }

    @Test
    void changePassword_WithoutCsrfToken_Returns403() throws Exception {
        mockMvc.perform(post("/api/users/change-password")
                .cookie(authCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequestDto("current123", "newpassword123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    void changePassword_WithCsrfToken_Returns204AndEmptyBody() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(post("/api/users/change-password")
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequestDto("current123", "newpassword123"))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).changePassword(any(User.class), any(ChangePasswordRequestDto.class));
    }

    @Test
    void changePassword_WrongCurrentPassword_Returns400WithCurrentPasswordFieldError() throws Exception {
        Cookie csrf = csrfCookie();
        doThrow(new InvalidPasswordException("currentPassword", "Current password is incorrect"))
                .when(userService).changePassword(any(User.class), any(ChangePasswordRequestDto.class));

        mockMvc.perform(post("/api/users/change-password")
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ChangePasswordRequestDto("wrong", "newpassword123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"))
                .andExpect(jsonPath("$.fieldErrors.currentPassword").value("Current password is incorrect"));
    }

    @Test
    void getUserCount_WithoutAuthentication_Returns200() throws Exception {
        when(userService.countUsers()).thenReturn(42L);

        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void csrfCookie_ShouldOutliveTheBrowserSession_WithTheSameLifetimeAsTheAuthCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/count")).andExpect(status().isOk()).andReturn();

        String setCookie = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no XSRF-TOKEN Set-Cookie header was sent"));

        assertThat(setCookie, containsString("Max-Age=3600"));
        assertThat(setCookie, containsString("Path=/"));
        assertThat(setCookie, not(containsString("HttpOnly")));
        assertThat(setCookie, not(containsString("Secure")));
    }

    @Test
    void anyRequest_RefreshesTheCsrfCookie_SoAnExpiredTokenIsAlwaysRecoverable() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/users/profile").cookie(authCookie()))
                .andExpect(status().isOk())
                .andReturn();
        Cookie issued = first.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(issued);

        MvcResult unauthenticated = mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(unauthenticated.getResponse().getCookie("XSRF-TOKEN"),
                "a client that lost its CSRF cookie must get a new one from any request");
    }

    @Test
    void changePassword_TooShortNewPassword_Returns400WithFieldErrors() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(post("/api/users/change-password")
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChangePasswordRequestDto("current123", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    void safeRequest_IssuesReadableCsrfCookie_SoSpaPostsAreNotRejected() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/profile").cookie(authCookie()))
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrf = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrf, "CsrfTokenRequestAttributeHandler must materialize the token on every response");
        org.junit.jupiter.api.Assertions.assertFalse(csrf.isHttpOnly(), "the SPA has to read XSRF-TOKEN from JS");
        org.junit.jupiter.api.Assertions.assertEquals("/", csrf.getPath());
    }
}
