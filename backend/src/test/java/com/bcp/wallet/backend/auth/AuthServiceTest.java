package com.bcp.wallet.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.bcp.wallet.backend.account.Account;
import com.bcp.wallet.backend.account.AccountRepository;
import com.bcp.wallet.backend.config.WalletProperties;
import com.bcp.wallet.backend.error.InvalidCredentialsException;
import com.bcp.wallet.backend.security.PasswordHasher;

class AuthServiceTest {

    private static final String PHONE = "987654321";
    private static final String PASSWORD = "clave-segura-1";

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordHasher currentHasher = hasherWith(1000);
    private final AuthService service = new AuthService(accounts, currentHasher);

    private static PasswordHasher hasherWith(int iterations) {
        return new PasswordHasher(new WalletProperties(null, new WalletProperties.Security(iterations), null));
    }

    private Account accountHashedWith(PasswordHasher hasher) {
        Account account = new Account(PHONE, hasher.hash(PASSWORD), "direct", Instant.parse("2026-10-30T12:00:00Z"));
        when(accounts.findByPhoneNumber(PHONE)).thenReturn(Optional.of(account));
        return account;
    }

    @Test
    void upgradesAnOutdatedHashAfterASuccessfulLogin() {
        Account account = accountHashedWith(hasherWith(500));

        service.login(new LoginRequest(PHONE, PASSWORD));

        assertThat(account.getPasswordHash()).startsWith("pbkdf2_sha256$1000$");
        assertThat(currentHasher.matches(PASSWORD, account.getPasswordHash())).isTrue();
    }

    @Test
    void keepsAnUpToDateHashUntouched() {
        Account account = accountHashedWith(currentHasher);
        String before = account.getPasswordHash();

        service.login(new LoginRequest(PHONE, PASSWORD));

        assertThat(account.getPasswordHash()).isEqualTo(before);
    }

    @Test
    void doesNotRehashWhenThePasswordIsWrong() {
        Account account = accountHashedWith(hasherWith(500));
        String before = account.getPasswordHash();

        assertThatThrownBy(() -> service.login(new LoginRequest(PHONE, "incorrecta")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(account.getPasswordHash()).isEqualTo(before);
    }

    @Test
    void rejectsAnUnknownPhone() {
        when(accounts.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest(PHONE, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
