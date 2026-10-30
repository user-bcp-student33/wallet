package com.bcp.wallet.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wallet")
public record WalletProperties(Cors cors, Security security, Analytics analytics) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Security(int pbkdf2Iterations) {
    }

    public record Analytics(String apiKey) {
    }
}
