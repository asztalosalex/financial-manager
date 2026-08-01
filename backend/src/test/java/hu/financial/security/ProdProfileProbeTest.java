package hu.financial.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = CookieProperties.class)
@ActiveProfiles({ "test", "prod" })
class ProdProfileProbeTest {

    @Autowired
    private CookieProperties cookieProperties;

    @Test
    void prodProfile_ActuallyLoadsApplicationProdProperties_AndForcesSecureCookies() {
        assertTrue(cookieProperties.isSecure(),
                "application-prod.properties must set security.cookie.secure=true and must actually be loaded");
    }
}
