package com.bcp.wallet.backend.onboarding;

import java.util.Optional;

public enum OnboardingStep {
    ACCOUNT_SETUP,
    WALLET_PREPARATION,
    WALLET_READY;

    public static int total() {
        return values().length;
    }

    public static Optional<OnboardingStep> afterCompleted(int completedSteps) {
        OnboardingStep[] steps = values();
        return completedSteps < steps.length ? Optional.of(steps[completedSteps]) : Optional.empty();
    }
}
