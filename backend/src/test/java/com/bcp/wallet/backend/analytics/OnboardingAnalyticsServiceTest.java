package com.bcp.wallet.backend.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.bcp.wallet.backend.config.WalletProperties;
import com.bcp.wallet.backend.error.AccessForbiddenException;
import com.bcp.wallet.backend.onboarding.CompletedOnboarding;
import com.bcp.wallet.backend.onboarding.OnboardingEventRepository;

class OnboardingAnalyticsServiceTest {

    private static final Instant START = Instant.parse("2026-10-10T15:00:00Z");

    private final OnboardingEventRepository events = mock(OnboardingEventRepository.class);
    private final AcquisitionSpendRepository spend = mock(AcquisitionSpendRepository.class);
    private final OnboardingAnalyticsService service = new OnboardingAnalyticsService(events, spend);

    @BeforeEach
    void noSpendByDefault() {
        when(spend.findBySpendMonth("2026-10")).thenReturn(List.of());
    }

    private static CompletedOnboarding completed(String channel, long seconds) {
        return new CompletedOnboarding(channel, START, START.plusSeconds(seconds));
    }

    private void givenCompleted(CompletedOnboarding... onboardings) {
        when(events.findCompletedBetween(any(), any())).thenReturn(List.of(onboardings));
    }

    @Test
    void medianOfAnOddNumberOfOnboardings() {
        givenCompleted(completed("direct", 600), completed("direct", 120), completed("direct", 180));
        when(events.countStartedBetween(any(), any())).thenReturn(5L);

        MonthlyOnboardingSummary summary = service.summarize(YearMonth.of(2026, 10));

        assertThat(summary.medianCompletionSeconds()).isEqualTo(180L);
        assertThat(summary.newActiveUsers()).isEqualTo(3);
        assertThat(summary.startedCount()).isEqualTo(5);
    }

    @Test
    void medianOfAnEvenNumberOfOnboardings() {
        givenCompleted(completed("direct", 120), completed("direct", 180));

        assertThat(service.summarize(YearMonth.of(2026, 10)).medianCompletionSeconds()).isEqualTo(150L);
    }

    @Test
    void emptyMonthHasNoMedianAndNoCac() {
        givenCompleted();

        MonthlyOnboardingSummary summary = service.summarize(YearMonth.of(2026, 10));

        assertThat(summary.newActiveUsers()).isZero();
        assertThat(summary.medianCompletionSeconds()).isNull();
        assertThat(summary.channels()).isEmpty();
        assertThat(summary.totals().cacPen()).isNull();
    }

    @Test
    void usesTheLimaCalendarMonthAsTheWindow() {
        givenCompleted();

        service.summarize(YearMonth.of(2026, 10));

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(events).findCompletedBetween(from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(Instant.parse("2026-10-01T05:00:00Z"));
        assertThat(to.getValue()).isEqualTo(Instant.parse("2026-11-01T05:00:00Z"));
    }

    @Test
    void computesCacPerChannelAndInTotal() {
        List<CompletedOnboarding> lima = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> completed("campania-lima", 100)).toList();
        List<CompletedOnboarding> all = new java.util.ArrayList<>(lima);
        all.add(completed("direct", 100));
        when(events.findCompletedBetween(any(), any())).thenReturn(all);
        when(spend.findBySpendMonth("2026-10")).thenReturn(List.of(
                new AcquisitionSpend("2026-10", "campania-lima", new BigDecimal("1000.00")),
                new AcquisitionSpend("2026-10", "campania-cusco", new BigDecimal("50.00"))));

        MonthlyOnboardingSummary summary = service.summarize(YearMonth.of(2026, 10));

        assertThat(summary.channels()).extracting(MonthlyOnboardingSummary.ChannelFigures::channel)
                .containsExactly("campania-cusco", "campania-lima", "direct");
        MonthlyOnboardingSummary.ChannelFigures cusco = summary.channels().get(0);
        assertThat(cusco.newActiveUsers()).isZero();
        assertThat(cusco.cacPen()).isNull();
        MonthlyOnboardingSummary.ChannelFigures limaFigures = summary.channels().get(1);
        assertThat(limaFigures.newActiveUsers()).isEqualTo(100);
        assertThat(limaFigures.cacPen()).isEqualByComparingTo("10.00");
        assertThat(summary.channels().get(2).acquisitionSpendPen()).isEqualByComparingTo("0.00");
        assertThat(summary.totals().newActiveUsers()).isEqualTo(101);
        assertThat(summary.totals().acquisitionSpendPen()).isEqualByComparingTo("1050.00");
        assertThat(summary.totals().cacPen()).isEqualByComparingTo("10.40");
    }

    @Test
    void accessGuardAcceptsOnlyTheConfiguredKey() {
        AnalyticsAccessGuard guard = new AnalyticsAccessGuard(
                new WalletProperties(null, null, new WalletProperties.Analytics("clave-analitica")));

        assertThatCode(() -> guard.verify("clave-analitica")).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.verify("otra")).isInstanceOf(AccessForbiddenException.class);
        assertThatThrownBy(() -> guard.verify(null)).isInstanceOf(AccessForbiddenException.class);
    }

    @Test
    void accessGuardDeniesEverythingWhenNoKeyIsConfigured() {
        AnalyticsAccessGuard guard = new AnalyticsAccessGuard(
                new WalletProperties(null, null, new WalletProperties.Analytics("")));

        assertThatThrownBy(() -> guard.verify("")).isInstanceOf(AccessForbiddenException.class);
        assertThatThrownBy(() -> guard.verify("cualquiera")).isInstanceOf(AccessForbiddenException.class);
    }
}
