package com.bcp.wallet.backend.analytics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Component;

import com.bcp.wallet.backend.config.WalletProperties;
import com.bcp.wallet.backend.error.AccessForbiddenException;

@Component
public class AnalyticsAccessGuard {

    private final byte[] expectedKey;

    public AnalyticsAccessGuard(WalletProperties properties) {
        this.expectedKey = properties.analytics().apiKey().getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String providedKey) {
        boolean keyConfigured = expectedKey.length > 0;
        if (!keyConfigured || providedKey == null
                || !MessageDigest.isEqual(expectedKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessForbiddenException();
        }
    }
}
