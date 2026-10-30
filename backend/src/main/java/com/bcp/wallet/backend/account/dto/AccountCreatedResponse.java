package com.bcp.wallet.backend.account.dto;

import java.util.UUID;

import com.bcp.wallet.backend.account.CardBrand;
import com.bcp.wallet.backend.onboarding.OnboardingStateResponse;

public record AccountCreatedResponse(UUID accountId, LinkedCardResponse card, OnboardingStateResponse onboarding) {

    public record LinkedCardResponse(CardBrand brand, String lastFour) {
    }
}
