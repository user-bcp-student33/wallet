package com.bcp.wallet.backend.account;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.bcp.wallet.backend.onboarding.OnboardingStep;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 9)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountStatus status;

    @Column(name = "completed_steps", nullable = false)
    private int completedSteps;

    @Column(name = "acquisition_channel", nullable = false, length = 40)
    private String acquisitionChannel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Account(String phoneNumber, String passwordHash, String acquisitionChannel, Instant createdAt) {
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.acquisitionChannel = acquisitionChannel;
        this.createdAt = createdAt;
        this.status = AccountStatus.PENDING_ONBOARDING;
    }

    public Optional<OnboardingStep> currentStep() {
        return OnboardingStep.afterCompleted(completedSteps);
    }

    public void completeCurrentStep() {
        completedSteps++;
        if (completedSteps == OnboardingStep.total()) {
            status = AccountStatus.ACTIVE;
        }
    }
}
