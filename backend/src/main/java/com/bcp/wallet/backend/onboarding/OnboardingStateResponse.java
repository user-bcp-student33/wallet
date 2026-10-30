package com.bcp.wallet.backend.onboarding;

import java.util.UUID;

import com.bcp.wallet.backend.account.Account;
import com.bcp.wallet.backend.account.AccountStatus;

public record OnboardingStateResponse(
        UUID accountId,
        AccountStatus status,
        int completedSteps,
        int totalSteps,
        OnboardingStep currentStep) {

    public static OnboardingStateResponse from(Account account) {
        return new OnboardingStateResponse(account.getId(), account.getStatus(), account.getCompletedSteps(),
                OnboardingStep.total(), account.currentStep().orElse(null));
    }
}
