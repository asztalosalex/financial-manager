package hu.financial.security;

import hu.financial.service.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityCookieFactoryTest {

    @Mock
    private JwtService jwtService;

    private SecurityCookieFactory factory(boolean secure, String sameSite) {
        return new SecurityCookieFactory(jwtService, new CookieProperties(secure, sameSite));
    }

    private Cookie emittedCsrfCookie(SecurityCookieFactory factory) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieCsrfTokenRepository repository = factory.csrfTokenRepository();
        repository.saveToken(repository.generateToken(request), request, response);

        Cookie cookie = response.getCookie("XSRF-TOKEN");
        assertNotNull(cookie, "no XSRF-TOKEN cookie was written");
        return cookie;
    }

    @Test
    void create_ShouldDeriveMaxAgeFromJwtExpiration_NotFromAConstant() {
        when(jwtService.getExpirationTime()).thenReturn(3600L);

        ResponseCookie cookie = factory(false, "Lax").createAuthCookie("token");

        assertEquals(3600L, cookie.getMaxAge().getSeconds());
        assertEquals("token", cookie.getValue());
    }

    @Test
    void create_ShouldAlwaysBeHttpOnlyAndRootScoped() {
        when(jwtService.getExpirationTime()).thenReturn(60L);

        ResponseCookie cookie = factory(false, "Lax").createAuthCookie("token");

        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals("authToken", cookie.getName());
    }

    @Test
    void create_ShouldApplyConfiguredSecureAndSameSite() {
        when(jwtService.getExpirationTime()).thenReturn(60L);

        ResponseCookie devCookie = factory(false, "Lax").createAuthCookie("token");
        ResponseCookie prodCookie = factory(true, "None").createAuthCookie("token");

        assertFalse(devCookie.isSecure());
        assertEquals("Lax", devCookie.getSameSite());
        assertTrue(prodCookie.isSecure());
        assertEquals("None", prodCookie.getSameSite());
    }

    @Test
    void csrfTokenRepository_ShouldOutliveTheBrowserSession_WithTheAuthCookieLifetime() {
        when(jwtService.getExpirationTime()).thenReturn(3600L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieCsrfTokenRepository repository = factory(true, "None").csrfTokenRepository();
        repository.saveToken(repository.generateToken(request), request, response);

        String setCookie = response.getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no XSRF-TOKEN Set-Cookie header was written"));

        assertTrue(setCookie.contains("Max-Age=3600"), setCookie);
        assertTrue(setCookie.contains("Path=/"), setCookie);
        assertTrue(setCookie.contains("Secure"), setCookie);
        assertFalse(setCookie.contains("HttpOnly"), setCookie);
    }

    @Test
    void csrfTokenRepository_ShouldApplyTheConfiguredSameSite_SoTheCrossSiteProdConfigNeedsNoCodeChange() {
        when(jwtService.getExpirationTime()).thenReturn(3600L);

        assertEquals("None", emittedCsrfCookie(factory(true, "None")).getAttribute("SameSite"));
        assertEquals("Lax", emittedCsrfCookie(factory(false, "Lax")).getAttribute("SameSite"));
    }

    @Test
    void csrfTokenRepository_ShouldUseTheSameSameSiteAsTheAuthCookie_SoNeitherIsWithheldWithoutTheOther() {
        when(jwtService.getExpirationTime()).thenReturn(3600L);
        SecurityCookieFactory factory = factory(true, "None");

        ResponseCookie authCookie = factory.createAuthCookie("token");

        assertEquals(authCookie.getSameSite(), emittedCsrfCookie(factory).getAttribute("SameSite"));
    }

    @Test
    void csrfTokenRepository_ShouldStillDeleteTheCookie_WhenTheTokenIsCleared() {
        when(jwtService.getExpirationTime()).thenReturn(3600L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        factory(false, "Lax").csrfTokenRepository().saveToken(null, request, response);

        String setCookie = response.getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no XSRF-TOKEN Set-Cookie header was written"));

        assertTrue(setCookie.contains("Max-Age=0"), setCookie);
    }

    @Test
    void expire_ShouldUseTheSameAttributesAsCreate_SoTheBrowserActuallyDeletesTheCookie() {
        when(jwtService.getExpirationTime()).thenReturn(3600L);
        SecurityCookieFactory factory = factory(true, "None");

        ResponseCookie created = factory.createAuthCookie("token");
        ResponseCookie expired = factory.expireAuthCookie();

        assertEquals(created.getName(), expired.getName());
        assertEquals(created.getPath(), expired.getPath());
        assertEquals(created.getSameSite(), expired.getSameSite());
        assertEquals(created.isSecure(), expired.isSecure());
        assertEquals(created.isHttpOnly(), expired.isHttpOnly());
        assertEquals(0L, expired.getMaxAge().getSeconds());
        assertEquals("", expired.getValue());
    }
}
