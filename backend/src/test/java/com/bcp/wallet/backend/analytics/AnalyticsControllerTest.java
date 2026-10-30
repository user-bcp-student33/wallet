package com.bcp.wallet.backend.analytics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bcp.wallet.backend.analytics.MonthlyOnboardingSummary.AcquisitionFigures;
import com.bcp.wallet.backend.analytics.MonthlyOnboardingSummary.ChannelFigures;

@WebMvcTest(AnalyticsController.class)
@Import(AnalyticsAccessGuard.class)
class AnalyticsControllerTest {

    private static final String KEY = "test-analytics-key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnboardingAnalyticsService analytics;

    @Test
    void returnsTheMonthlySummaryWithTheConfiguredKey() throws Exception {
        when(analytics.summarize(YearMonth.of(2026, 10))).thenReturn(new MonthlyOnboardingSummary("2026-10", 120, 100,
                180L, List.of(ChannelFigures.of("campania-lima", 100, new BigDecimal("1000"))),
                AcquisitionFigures.of(100, new BigDecimal("1000"))));

        mockMvc.perform(get("/analytics/onboarding/monthly").param("month", "2026-10")
                .header("X-Analytics-Key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.startedCount").value(120))
                .andExpect(jsonPath("$.newActiveUsers").value(100))
                .andExpect(jsonPath("$.medianCompletionSeconds").value(180))
                .andExpect(jsonPath("$.channels[0].cacPen").value(10.0))
                .andExpect(jsonPath("$.totals.cacPen").value(10.0));
    }

    @Test
    void rejectsAMissingKey() throws Exception {
        mockMvc.perform(get("/analytics/onboarding/monthly").param("month", "2026-10"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(analytics);
    }

    @Test
    void rejectsAWrongKey() throws Exception {
        mockMvc.perform(get("/analytics/onboarding/monthly").param("month", "2026-10")
                .header("X-Analytics-Key", "otra-clave"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(analytics);
    }

    @Test
    void rejectsAnInvalidMonthFormat() throws Exception {
        mockMvc.perform(get("/analytics/onboarding/monthly").param("month", "octubre")
                .header("X-Analytics-Key", KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("month"));
        verifyNoInteractions(analytics);
    }

    @Test
    void rejectsAMissingMonth() throws Exception {
        mockMvc.perform(get("/analytics/onboarding/monthly").header("X-Analytics-Key", KEY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(analytics);
        org.mockito.Mockito.verify(analytics, org.mockito.Mockito.never()).summarize(any());
    }
}
