package com.bcp.wallet.backend.account;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "linked_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkedCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardBrand brand;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public LinkedCard(Account account, CardBrand brand, String lastFour, Instant createdAt) {
        this.account = account;
        this.brand = brand;
        this.lastFour = lastFour;
        this.createdAt = createdAt;
    }
}
