package hu.financial.security;

import hu.financial.service.JwtService;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SecurityCookieFactory {

    public static final String AUTH_COOKIE_NAME = "authToken";

    private final JwtService jwtService;
    private final CookieProperties cookieProperties;

    public SecurityCookieFactory(JwtService jwtService, CookieProperties cookieProperties) {
        this.jwtService = jwtService;
        this.cookieProperties = cookieProperties;
    }

    public ResponseCookie createAuthCookie(String token) {
        return authCookieBuilder(token)
                .maxAge(lifetime())
                .build();
    }

    public ResponseCookie expireAuthCookie() {
        return authCookieBuilder("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(builder -> {
            boolean clearing = builder.build().getValue().isEmpty();
            builder.secure(cookieProperties.isSecure())
            .sameSite(cookieProperties.getSameSite())
            .maxAge(clearing ? Duration.ZERO : lifetime());
        });
        return repository;
    }

    private Duration lifetime() {
        return Duration.ofSeconds(jwtService.getExpirationTime());
    }

    private ResponseCookie.ResponseCookieBuilder authCookieBuilder(String value) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/");
    }
}
