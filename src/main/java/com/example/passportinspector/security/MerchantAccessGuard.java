package com.example.passportinspector.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("merchantAccessGuard")
public class MerchantAccessGuard {
    public boolean hasMerchantId(Authentication authentication, String merchantId) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }
        String tokenMerchantId = jwtAuthentication.getToken().getClaimAsString("merchantId");
        return merchantId != null && merchantId.equals(tokenMerchantId);
    }
}