package com.bcp.wallet.backend.onboarding;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bcp.wallet.backend.account.Account;
import com.bcp.wallet.backend.account.AccountRepository;
import com.bcp.wallet.backend.error.AccountNotFoundException;
import com.bcp.wallet.backend.error.StepConflictException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final AccountRepository accounts;
    private final OnboardingEventRepository events;
    private final Clock clock;

    @Transactional
    public void begin(Account account) {
        record(account, OnboardingEventType.ONBOARDING_STARTED, null);
        completeCurrentStep(account);
    }

    @Transactional(readOnly = true)
    public OnboardingStateResponse stateOf(UUID accountId) {
        return OnboardingStateResponse.from(find(accountId));
    }

    @Transactional
    public OnboardingStateResponse complete(UUID accountId, OnboardingStep step) {
        Account account = find(accountId);
        if (account.currentStep().filter(step::equals).isEmpty()) {
            throw new StepConflictException();
        }
        completeCurrentStep(account);
        return OnboardingStateResponse.from(account);
    }

    private void completeCurrentStep(Account account) {
        OnboardingStep step = account.currentStep().orElseThrow(StepConflictException::new);
        account.completeCurrentStep();
        record(account, OnboardingEventType.STEP_COMPLETED, step);
        if (account.currentStep().isEmpty()) {
            record(account, OnboardingEventType.ONBOARDING_COMPLETED, null);
        }
    }

    private void record(Account account, OnboardingEventType type, OnboardingStep step) {
        events.save(new OnboardingEvent(account, type, step, clock.instant()));
    }

    private Account find(UUID accountId) {
        return accounts.findById(accountId).orElseThrow(AccountNotFoundException::new);
    }
}
