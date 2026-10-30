package com.bcp.wallet.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.bcp.wallet.backend.analytics.AcquisitionSpend;
import com.bcp.wallet.backend.analytics.AcquisitionSpendRepository;
import com.bcp.wallet.backend.analytics.OnboardingAnalyticsService;
import com.bcp.wallet.backend.onboarding.OnboardingEvent;
import com.bcp.wallet.backend.onboarding.OnboardingEventRepository;
import com.bcp.wallet.backend.onboarding.OnboardingEventType;
import com.bcp.wallet.backend.onboarding.OnboardingStep;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OnboardingFlowIntegrationTest {

    private static final String CARD = "4111111111111111";
    private static final String PASSWORD = "clave-segura-1";
    private static final String ANALYTICS_KEY = "test-analytics-key";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private OnboardingEventRepository events;
    @Autowired
    private AcquisitionSpendRepository spend;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("delete from onboarding_events");
        jdbc.update("delete from linked_cards");
        jdbc.update("delete from acquisition_spend");
        jdbc.update("delete from accounts");
    }

    private static String registration(String phone, String channelJson) {
        return """
                {"cardNumber":"%s","cvv":"123","phoneNumber":"%s","password":"%s"%s}
                """.formatted(CARD, phone, PASSWORD, channelJson);
    }

    private MvcResult register(String phone, String channelJson, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(registration(phone, channelJson))).andExpect(status().is(expectedStatus)).andReturn();
    }

    private UUID registerAccount(String phone, String channelJson) throws Exception {
        JsonNode body = JSON.readTree(register(phone, channelJson, 201).getResponse().getContentAsString());
        return UUID.fromString(body.get("accountId").asString());
    }

    private void complete(UUID accountId, OnboardingStep step, int expectedStatus) throws Exception {
        mockMvc.perform(post("/accounts/{id}/onboarding/steps/{step}/complete", accountId, step))
                .andExpect(status().is(expectedStatus));
    }

    private void activate(UUID accountId) throws Exception {
        complete(accountId, OnboardingStep.WALLET_PREPARATION, 200);
        complete(accountId, OnboardingStep.WALLET_READY, 200);
    }

    private MvcResult login(String phone, String password, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"%s\",\"password\":\"%s\"}".formatted(phone, password)))
                .andExpect(status().is(expectedStatus)).andReturn();
    }

    @Test
    void completesTheWholeOnboardingAndActivatesTheWallet() throws Exception {
        UUID accountId = registerAccount("987654321", "");

        mockMvc.perform(get("/accounts/{id}/onboarding", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_ONBOARDING"))
                .andExpect(jsonPath("$.completedSteps").value(1))
                .andExpect(jsonPath("$.currentStep").value("WALLET_PREPARATION"));
        complete(accountId, OnboardingStep.WALLET_READY, 409);
        complete(accountId, OnboardingStep.ACCOUNT_SETUP, 409);
        assertThat(events.findByAccountIdOrderByOccurredAtAsc(accountId)).hasSize(2);

        complete(accountId, OnboardingStep.WALLET_PREPARATION, 200);
        mockMvc.perform(post("/accounts/{id}/onboarding/steps/WALLET_READY/complete", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.completedSteps").value(3))
                .andExpect(jsonPath("$.currentStep").doesNotExist());
        complete(accountId, OnboardingStep.WALLET_READY, 409);

        List<OnboardingEvent> recorded = events.findByAccountIdOrderByOccurredAtAsc(accountId);
        assertThat(recorded).extracting(OnboardingEvent::getType).containsExactlyInAnyOrder(
                OnboardingEventType.ONBOARDING_STARTED, OnboardingEventType.STEP_COMPLETED,
                OnboardingEventType.STEP_COMPLETED, OnboardingEventType.STEP_COMPLETED,
                OnboardingEventType.ONBOARDING_COMPLETED);
        assertThat(recorded).filteredOn(e -> e.getType() == OnboardingEventType.STEP_COMPLETED)
                .extracting(OnboardingEvent::getStep).containsExactlyInAnyOrder(OnboardingStep.values());
    }

    @Test
    void loginAfterRegistrationReturnsTheSameAccountAndItsPendingState() throws Exception {
        UUID accountId = registerAccount("987654321", "");

        MvcResult pending = login("987654321", PASSWORD, 200);
        assertThat(JSON.readTree(pending.getResponse().getContentAsString()).get("accountId").asString())
                .isEqualTo(accountId.toString());
        assertThat(pending.getResponse().getContentAsString()).contains("PENDING_ONBOARDING")
                .doesNotContain(PASSWORD).doesNotContain("passwordHash");

        activate(accountId);
        assertThat(login("987654321", PASSWORD, 200).getResponse().getContentAsString()).contains("\"ACTIVE\"");
    }

    @Test
    void rejectsWrongPasswordAndUnknownPhoneWithTheSameBody() throws Exception {
        registerAccount("987654321", "");

        String wrongPassword = login("987654321", "equivocada", 401).getResponse().getContentAsString();
        String unknownPhone = login("911111111", "equivocada", 401).getResponse().getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknownPhone);
    }

    @Test
    void rejectsADuplicatedPhoneAndKeepsASingleAccount() throws Exception {
        registerAccount("987654321", "");

        register("987654321", "", 409);

        assertThat(jdbc.queryForObject("select count(*) from accounts", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select count(*) from linked_cards", Long.class)).isEqualTo(1L);
    }

    @Test
    void storesOnlyAHashOfThePasswordAndTheLastFourCardDigits() throws Exception {
        registerAccount("987654321", "");
        registerAccount("912345678", "");

        List<String> hashes = jdbc.queryForList("select password_hash from accounts", String.class);
        assertThat(hashes).hasSize(2).doesNotContain(PASSWORD).doesNotHaveDuplicates();
        assertThat(hashes).allSatisfy(hash -> assertThat(hash).doesNotContain(PASSWORD));
        assertThat(jdbc.queryForList("select last_four from linked_cards", String.class)).containsOnly("1111");
    }

    @Test
    void persistsNeitherTheCvvNorTheFullCardNumber() throws Exception {
        registerAccount("987654321", "");

        for (String table : List.of("accounts", "linked_cards", "onboarding_events")) {
            List<String> columns = jdbc.queryForList(
                    "select lower(column_name) from information_schema.columns where lower(table_name) = ?",
                    String.class, table);
            assertThat(columns).noneMatch(column -> column.contains("cvv") || column.contains("card_number"));
            for (Map<String, Object> row : jdbc.queryForList("select * from " + table)) {
                assertThat(row.values().toString()).doesNotContain(CARD);
            }
        }
    }

    @Test
    void returnsNotFoundForAnUnknownAccount() throws Exception {
        mockMvc.perform(get("/accounts/{id}/onboarding", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        complete(UUID.randomUUID(), OnboardingStep.WALLET_PREPARATION, 404);
    }

    @Test
    void summarizesTheMonthWithNewActiveUsersAndCacPerChannel() throws Exception {
        String month = YearMonth.now(OnboardingAnalyticsServiceZone.LIMA).toString();
        UUID first = registerAccount("987654321", ",\"acquisitionChannel\":\"campania-lima\"");
        UUID second = registerAccount("912345678", ",\"acquisitionChannel\":\"campania-lima\"");
        registerAccount("955443322", "");
        activate(first);
        activate(second);
        spend.save(new AcquisitionSpend(month, "campania-lima", new BigDecimal("30.00")));

        mockMvc.perform(get("/analytics/onboarding/monthly").param("month", month)
                .header("X-Analytics-Key", ANALYTICS_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startedCount").value(3))
                .andExpect(jsonPath("$.newActiveUsers").value(2))
                .andExpect(jsonPath("$.medianCompletionSeconds").isNumber())
                .andExpect(jsonPath("$.channels[0].channel").value("campania-lima"))
                .andExpect(jsonPath("$.channels[0].newActiveUsers").value(2))
                .andExpect(jsonPath("$.channels[0].cacPen").value(15.0))
                .andExpect(jsonPath("$.totals.acquisitionSpendPen").value(30.0))
                .andExpect(jsonPath("$.totals.cacPen").value(15.0));
        mockMvc.perform(get("/analytics/onboarding/monthly").param("month", month))
                .andExpect(status().isForbidden());
    }

    private static final class OnboardingAnalyticsServiceZone {
        static final java.time.ZoneId LIMA = java.time.ZoneId.of("America/Lima");
    }
}
