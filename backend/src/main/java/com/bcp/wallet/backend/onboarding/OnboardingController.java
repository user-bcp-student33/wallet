package com.bcp.wallet.backend.onboarding;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts/{accountId}/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboarding;

    @GetMapping
    public OnboardingStateResponse state(@PathVariable UUID accountId) {
        return onboarding.stateOf(accountId);
    }

    @PostMapping("/steps/{step}/complete")
    public OnboardingStateResponse completeStep(@PathVariable UUID accountId, @PathVariable OnboardingStep step) {
        return onboarding.complete(accountId, step);
    }
}
