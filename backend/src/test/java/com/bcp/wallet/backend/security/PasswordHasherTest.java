package com.bcp.wallet.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.bcp.wallet.backend.config.WalletProperties;

class PasswordHasherTest {

    private static final String PASSWORD = "clave-segura-123";

    private final PasswordHasher hasher = new PasswordHasher(
            new WalletProperties(null, new WalletProperties.Security(1000), null));

    @Test
    void twoHashesOfTheSamePasswordDiffer() {
        assertThat(hasher.hash(PASSWORD)).isNotEqualTo(hasher.hash(PASSWORD));
    }

    @Test
    void storedHashNeverContainsThePlainPassword() {
        assertThat(hasher.hash(PASSWORD)).doesNotContain(PASSWORD).startsWith("pbkdf2_sha256$1000$");
    }

    @Test
    void acceptsTheCorrectPassword() {
        assertThat(hasher.matches(PASSWORD, hasher.hash(PASSWORD))).isTrue();
    }

    @Test
    void rejectsAWrongPassword() {
        assertThat(hasher.matches("otra-clave", hasher.hash(PASSWORD))).isFalse();
    }

    @Test
    void rejectsMalformedStoredHashes() {
        assertThat(hasher.matches(PASSWORD, "texto-plano")).isFalse();
        assertThat(hasher.matches(PASSWORD, "otro$1000$AAAA$AAAA")).isFalse();
        assertThat(hasher.matches(PASSWORD, "pbkdf2_sha256$no-numero$AAAA$AAAA")).isFalse();
        assertThat(hasher.matches(PASSWORD, "pbkdf2_sha256$1000$***$***")).isFalse();
    }
}
