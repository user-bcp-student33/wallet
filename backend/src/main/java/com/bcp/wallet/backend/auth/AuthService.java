package com.bcp.wallet.backend.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bcp.wallet.backend.account.Account;
import com.bcp.wallet.backend.account.AccountRepository;
import com.bcp.wallet.backend.error.InvalidCredentialsException;
import com.bcp.wallet.backend.security.PasswordHasher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accounts;
    private final PasswordHasher passwordHasher;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        return accounts.findByPhoneNumber(request.phoneNumber())
                .filter(account -> passwordHasher.matches(request.password(), account.getPasswordHash()))
                .map(account -> upgradeHashIfOutdated(account, request.password()))
                .map(LoginResponse::from)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private Account upgradeHashIfOutdated(Account account, String password) {
        if (passwordHasher.needsRehash(account.getPasswordHash())) {
            account.replacePasswordHash(passwordHasher.hash(password));
        }
        return account;
    }
}
