package hu.financial.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieProperties {

    private final boolean secure;
    private final String sameSite;

    public CookieProperties(
            @Value("${security.cookie.secure:true}") boolean secure,
            @Value("${security.cookie.same-site:Lax}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public boolean isSecure() {
        return secure;
    }

    public String getSameSite() {
        return sameSite;
    }
}
