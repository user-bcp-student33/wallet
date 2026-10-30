package com.bcp.wallet.backend.onboarding;

import java.time.Instant;
import java.util.UUID;

import com.bcp.wallet.backend.account.Account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "onboarding_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnboardingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OnboardingEventType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OnboardingStep step;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public OnboardingEvent(Account account, OnboardingEventType type, OnboardingStep step, Instant occurredAt) {
        this.account = account;
        this.type = type;
        this.step = step;
        this.occurredAt = occurredAt;
    }
}
