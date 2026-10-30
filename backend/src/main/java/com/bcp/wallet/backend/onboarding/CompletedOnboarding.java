package com.bcp.wallet.backend.onboarding;

import java.time.Instant;

public record CompletedOnboarding(String acquisitionChannel, Instant startedAt, Instant completedAt) {
}
