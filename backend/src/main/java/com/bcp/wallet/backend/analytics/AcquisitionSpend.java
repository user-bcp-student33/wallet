package com.bcp.wallet.backend.analytics;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "acquisition_spend", uniqueConstraints = @UniqueConstraint(columnNames = { "spend_month", "channel" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcquisitionSpend {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "spend_month", nullable = false, length = 7)
    private String spendMonth;

    @Column(nullable = false, length = 40)
    private String channel;

    @Column(name = "amount_pen", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountPen;

    public AcquisitionSpend(String spendMonth, String channel, BigDecimal amountPen) {
        this.spendMonth = spendMonth;
        this.channel = channel;
        this.amountPen = amountPen;
    }
}
