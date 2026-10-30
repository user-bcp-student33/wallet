package com.bcp.wallet.backend.analytics;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bcp.wallet.backend.analytics.MonthlyOnboardingSummary.AcquisitionFigures;
import com.bcp.wallet.backend.analytics.MonthlyOnboardingSummary.ChannelFigures;
import com.bcp.wallet.backend.onboarding.CompletedOnboarding;
import com.bcp.wallet.backend.onboarding.OnboardingEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingAnalyticsService {

    static final ZoneId REPORTING_ZONE = ZoneId.of("America/Lima");

    private final OnboardingEventRepository events;
    private final AcquisitionSpendRepository spendRepository;

    @Transactional(readOnly = true)
    public MonthlyOnboardingSummary summarize(YearMonth month) {
        Instant from = month.atDay(1).atStartOfDay(REPORTING_ZONE).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(REPORTING_ZONE).toInstant();
        List<CompletedOnboarding> completed = events.findCompletedBetween(from, to);
        Map<String, BigDecimal> spendByChannel = spendByChannel(month);

        List<ChannelFigures> channels = channelsOf(completed, spendByChannel);
        BigDecimal totalSpend = spendByChannel.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthlyOnboardingSummary(
                month.toString(),
                events.countStartedBetween(from, to),
                completed.size(),
                medianSeconds(completed),
                channels,
                AcquisitionFigures.of(completed.size(), totalSpend));
    }

    private Map<String, BigDecimal> spendByChannel(YearMonth month) {
        return spendRepository.findBySpendMonth(month.toString()).stream()
                .collect(Collectors.toMap(AcquisitionSpend::getChannel, AcquisitionSpend::getAmountPen,
                        BigDecimal::add, TreeMap::new));
    }

    private static List<ChannelFigures> channelsOf(List<CompletedOnboarding> completed,
            Map<String, BigDecimal> spendByChannel) {
        Map<String, Long> usersByChannel = completed.stream()
                .collect(Collectors.groupingBy(CompletedOnboarding::acquisitionChannel, Collectors.counting()));
        TreeSet<String> channelNames = new TreeSet<>(usersByChannel.keySet());
        channelNames.addAll(spendByChannel.keySet());
        return channelNames.stream()
                .map(channel -> ChannelFigures.of(channel, usersByChannel.getOrDefault(channel, 0L),
                        spendByChannel.getOrDefault(channel, BigDecimal.ZERO)))
                .toList();
    }

    static Long medianSeconds(List<CompletedOnboarding> completed) {
        if (completed.isEmpty()) {
            return null;
        }
        List<Long> durations = completed.stream()
                .map(onboarding -> Duration.between(onboarding.startedAt(), onboarding.completedAt()).toSeconds())
                .sorted()
                .toList();
        int middle = durations.size() / 2;
        return durations.size() % 2 == 1
                ? durations.get(middle)
                : Math.round((durations.get(middle - 1) + durations.get(middle)) / 2.0);
    }
}
