package com.bcp.wallet.backend.onboarding;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OnboardingEventRepository extends JpaRepository<OnboardingEvent, UUID> {

    List<OnboardingEvent> findByAccountIdOrderByOccurredAtAsc(UUID accountId);

    @Query("""
            select count(e) from OnboardingEvent e
            where e.type = com.bcp.wallet.backend.onboarding.OnboardingEventType.ONBOARDING_STARTED
              and e.occurredAt >= :from and e.occurredAt < :to
            """)
    long countStartedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select new com.bcp.wallet.backend.onboarding.CompletedOnboarding(a.acquisitionChannel, s.occurredAt, c.occurredAt)
            from OnboardingEvent c
            join OnboardingEvent s on s.account = c.account
            join c.account a
            where c.type = com.bcp.wallet.backend.onboarding.OnboardingEventType.ONBOARDING_COMPLETED
              and s.type = com.bcp.wallet.backend.onboarding.OnboardingEventType.ONBOARDING_STARTED
              and c.occurredAt >= :from and c.occurredAt < :to
            """)
    List<CompletedOnboarding> findCompletedBetween(@Param("from") Instant from, @Param("to") Instant to);
}
