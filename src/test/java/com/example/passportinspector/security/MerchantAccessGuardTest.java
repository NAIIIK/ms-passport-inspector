package com.example.passportinspector.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantAccessGuardTest {

    private static final String MERCHANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String OTHER_MERCHANT_ID = "660e8400-e29b-41d4-a716-446655440111";

    private final MerchantAccessGuard guard = new MerchantAccessGuard();

    @Test
    void shouldReturnTrueWhenTokenMerchantIdMatches() {
        Authentication auth = jwtAuthWithMerchantId(MERCHANT_ID);

        assertThat(guard.hasMerchantId(auth, MERCHANT_ID)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTokenMerchantIdDoesNotMatch() {
        Authentication auth = jwtAuthWithMerchantId(OTHER_MERCHANT_ID);

        assertThat(guard.hasMerchantId(auth, MERCHANT_ID)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenRequestedMerchantIdIsNull() {
        Authentication auth = jwtAuthWithMerchantId(MERCHANT_ID);

        assertThat(guard.hasMerchantId(auth, null)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenTokenHasNoMerchantIdClaim() {
        Authentication auth = jwtAuthWithMerchantId(null);

        assertThat(guard.hasMerchantId(auth, MERCHANT_ID)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenAuthenticationIsNotJwtBased() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "password");

        assertThat(guard.hasMerchantId(auth, MERCHANT_ID)).isFalse();
    }

    private JwtAuthenticationToken jwtAuthWithMerchantId(String merchantId) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("demo")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));

        if (merchantId != null) {
            builder.claim("merchantId", merchantId);
        } else {
            builder.claim("placeholder", "value");
        }

        return new JwtAuthenticationToken(builder.build(), List.of());
    }
}