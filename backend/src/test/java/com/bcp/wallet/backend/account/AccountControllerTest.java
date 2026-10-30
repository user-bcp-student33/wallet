package com.bcp.wallet.backend.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.bcp.wallet.backend.account.dto.AccountCreatedResponse;
import com.bcp.wallet.backend.account.dto.AccountCreatedResponse.LinkedCardResponse;
import com.bcp.wallet.backend.account.CreateAccountCommand;
import com.bcp.wallet.backend.error.DuplicatePhoneException;
import com.bcp.wallet.backend.onboarding.OnboardingStateResponse;
import com.bcp.wallet.backend.onboarding.OnboardingStep;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final String CONFIGURED_ORIGIN = "http://localhost:4200";
    private static final String PASSWORD = "clave-secreta-1";
    private static final String CVV = "123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    private static String body(String card, String cvv, String phone, String password, String channelJson) {
        return """
                {"cardNumber":"%s","cvv":"%s","phoneNumber":"%s","password":"%s"%s}
                """.formatted(card, cvv, phone, password, channelJson);
    }

    private static String validBody() {
        return body("4111111111111111", CVV, "987654321", PASSWORD, "");
    }

    private MvcResult register(String json) throws Exception {
        return mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
    }

    @Test
    void returnsCreatedWithAccountCardAndOnboardingState() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.register(any())).thenReturn(new AccountCreatedResponse(accountId,
                new LinkedCardResponse(CardBrand.VISA, "1111"),
                new OnboardingStateResponse(accountId, AccountStatus.PENDING_ONBOARDING, 1, 3,
                        OnboardingStep.WALLET_PREPARATION)));

        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.card.brand").value("VISA"))
                .andExpect(jsonPath("$.card.lastFour").value("1111"))
                .andExpect(jsonPath("$.onboarding.status").value("PENDING_ONBOARDING"))
                .andExpect(jsonPath("$.onboarding.completedSteps").value(1))
                .andExpect(jsonPath("$.onboarding.totalSteps").value(3))
                .andExpect(jsonPath("$.onboarding.currentStep").value("WALLET_PREPARATION"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("4111111111111111"))));
    }

    @ParameterizedTest
    @ValueSource(strings = { "98765432", "9876543210", "887654321", "987 654 321", "+51987654321" })
    void rejectsInvalidPhonesWithAnActionableMessage(String phone) throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111", CVV, phone, PASSWORD, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phoneNumber"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(AccountRules.PHONE_MESSAGE))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(PASSWORD))));
        verifyNoInteractions(accountService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc1234", "1234567" })
    void rejectsPasswordsShorterThanEightCharacters(String password) throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111", CVV, "987654321", password, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(AccountRules.PASSWORD_MESSAGE));
    }

    @Test
    void rejectsPasswordsLongerThan128Characters() throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111", CVV, "987654321", "a".repeat(129), "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 8, 128 })
    void acceptsPasswordsOfAnyCompositionBetween8And128Characters(int length) throws Exception {
        when(accountService.register(any())).thenReturn(null);

        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111", CVV, "987654321", "a".repeat(length), "")))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = { "12", "1234", "12a" })
    void rejectsInvalidCvv(String cvv) throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111", cvv, "987654321", PASSWORD, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cvv"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(AccountRules.CVV_MESSAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = { "4111 1111 1111 1111", "41111111111111111", "4111-1111", "abcd" })
    void rejectsCardsWithNonDigitsOrMoreThan16Digits(String card) throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body(card, CVV, "987654321", PASSWORD, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cardNumber"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(AccountRules.CARD_FORMAT_MESSAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "1234567890123456", "4111111111111112" })
    void acceptsAnyDigitsOnlyCardUpTo16Digits(String card) throws Exception {
        when(accountService.register(any())).thenReturn(null);

        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body(card, CVV, "987654321", PASSWORD, "")))
                .andExpect(status().isCreated());
    }

    @Test
    void reportsEveryMissingField() throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(4))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cardNumber"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("cvv"))
                .andExpect(jsonPath("$.fieldErrors[2].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[3].field").value("phoneNumber"));
        verifyNoInteractions(accountService);
    }

    @Test
    void reportsSeveralInvalidFieldsAtOnce() throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111", CVV, "98765432", "12345", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("phoneNumber"));
    }

    @Test
    void passesTheAcquisitionChannelToTheService() throws Exception {
        when(accountService.register(any())).thenReturn(null);

        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111", CVV, "987654321", PASSWORD,
                        ",\"acquisitionChannel\":\"campania-lima\"")))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateAccountCommand> captor = ArgumentCaptor.forClass(CreateAccountCommand.class);
        verify(accountService).register(captor.capture());
        assertThat(captor.getValue().acquisitionChannel()).isEqualTo("campania-lima");
    }

    @Test
    void rejectsAnInvalidAcquisitionChannel() throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                .content(body("4111111111111111", CVV, "987654321", PASSWORD, ",\"acquisitionChannel\":\"Campania Lima\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("acquisitionChannel"));
    }

    @Test
    void rejectsMalformedJsonWithoutDetails() throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void translatesADuplicatedPhoneToConflict() throws Exception {
        when(accountService.register(any())).thenThrow(new DuplicatePhoneException());

        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Este número de celular ya está en uso."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("987654321"))));
    }

    @Test
    void hidesInternalErrorDetails() throws Exception {
        when(accountService.register(any()))
                .thenThrow(new IllegalStateException("select * from accounts where password='" + PASSWORD + "'"));

        MvcResult result = register(validBody());

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(result.getResponse().getContentAsString())
                .contains("INTERNAL_ERROR")
                .doesNotContain("select")
                .doesNotContain(PASSWORD)
                .doesNotContain("IllegalStateException");
    }

    @Test
    void allowsPreflightFromTheConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/accounts")
                .header("Origin", CONFIGURED_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", CONFIGURED_ORIGIN));
    }

    @Test
    void rejectsPreflightFromAnotherOrigin() throws Exception {
        mockMvc.perform(options("/accounts")
                .header("Origin", "http://malicioso.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
