package com.bcp.wallet.backend.auth;

import java.util.UUID;

import com.bcp.wallet.backend.account.Account;
import com.bcp.wallet.backend.account.AccountStatus;
import com.bcp.wallet.backend.onboarding.OnboardingStateResponse;

public record LoginResponse(UUID accountId, String phoneNumber, AccountStatus status,
        OnboardingStateResponse onboarding) {

    public static LoginResponse from(Account account) {
        return new LoginResponse(account.getId(), account.getPhoneNumber(), account.getStatus(),
                OnboardingStateResponse.from(account));
    }
}
