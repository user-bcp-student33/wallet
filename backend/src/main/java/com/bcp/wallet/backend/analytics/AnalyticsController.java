package com.bcp.wallet.backend.analytics;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bcp.wallet.backend.error.InvalidFieldException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/analytics/onboarding")
@RequiredArgsConstructor
public class AnalyticsController {

    static final String KEY_HEADER = "X-Analytics-Key";
    static final String MONTH_FIELD = "month";
    static final String MONTH_MESSAGE = "Usa el formato YYYY-MM, por ejemplo 2026-10.";

    private final AnalyticsAccessGuard accessGuard;
    private final OnboardingAnalyticsService analytics;

    @GetMapping("/monthly")
    public MonthlyOnboardingSummary monthly(
            @RequestHeader(name = KEY_HEADER, required = false) String apiKey,
            @RequestParam(MONTH_FIELD) String month) {
        accessGuard.verify(apiKey);
        return analytics.summarize(parseMonth(month));
    }

    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException invalidFormat) {
            throw new InvalidFieldException(MONTH_FIELD, MONTH_MESSAGE);
        }
    }
}
