package hu.financial.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(classes = CookieProperties.class)
@ActiveProfiles("test")
class TestProfileProbeTest {

    @Autowired
    private CookieProperties cookieProperties;

    @Test
    void testProfile_SuppliesTheInsecureFallback_ThatMakesTheProdProbeAbleToFail() {
        assertFalse(cookieProperties.isSecure(),
                "application-test.properties must keep SECURITY_COOKIE_SECURE=false; "
                        + "ProdProfileProbeTest can only fail while this fallback differs from the prod override");
    }

    @Test
    void testProfile_SuppliesTheNonDefaultSameSite_ThatMakesTheProdProbeAbleToFail() {
        assertEquals("Strict", cookieProperties.getSameSite(),
                "application-test.properties must keep SECURITY_COOKIE_SAME_SITE=Strict; "
                        + "Lax is the placeholder default in application.properties, so restoring it "
                        + "makes ProdProfileProbeTest pass even when application-prod.properties is not loaded");
    }
}
