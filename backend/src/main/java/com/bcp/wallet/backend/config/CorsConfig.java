package com.bcp.wallet.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String[] ALLOWED_PATHS = { "/accounts/**", "/auth/login" };
    private static final String[] ALLOWED_METHODS = { "GET", "POST", "OPTIONS" };
    private static final String[] ALLOWED_HEADERS = { "Content-Type" };

    private final String[] allowedOrigins;

    public CorsConfig(WalletProperties properties) {
        this.allowedOrigins = properties.cors().allowedOrigins().toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        for (String path : ALLOWED_PATHS) {
            registry.addMapping(path)
                    .allowedOrigins(allowedOrigins)
                    .allowedMethods(ALLOWED_METHODS)
                    .allowedHeaders(ALLOWED_HEADERS);
        }
    }
}
