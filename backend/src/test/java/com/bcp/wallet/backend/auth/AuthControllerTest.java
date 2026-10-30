package com.bcp.wallet.backend.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bcp.wallet.backend.account.AccountStatus;
import com.bcp.wallet.backend.error.InvalidCredentialsException;
import com.bcp.wallet.backend.onboarding.OnboardingStateResponse;
import com.bcp.wallet.backend.onboarding.OnboardingStep;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String PHONE = "987654321";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static String body(String phone, String password) {
        return """
                {"phoneNumber":"%s","password":"%s"}
                """.formatted(phone, password);
    }

    @Test
    void returnsTheUserStatusAndOnboardingWithoutCredentialsOrSession() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(authService.login(any())).thenReturn(new LoginResponse(accountId, PHONE,
                AccountStatus.PENDING_ONBOARDING, new OnboardingStateResponse(accountId,
                        AccountStatus.PENDING_ONBOARDING, 1, 3, OnboardingStep.WALLET_PREPARATION)));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(PHONE, "secreta-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.phoneNumber").value(PHONE))
                .andExpect(jsonPath("$.status").value("PENDING_ONBOARDING"))
                .andExpect(jsonPath("$.onboarding.completedSteps").value(1))
                .andExpect(jsonPath("$.onboarding.currentStep").value("WALLET_PREPARATION"))
                .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(content().string(not(containsString("token"))))
                .andExpect(content().string(not(containsString("secreta-1"))))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void usesTheSameBodyForUnknownPhoneAndWrongPassword() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        String unknownPhone = mockMvc
                .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("911111111", "una-clave")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String wrongPassword = mockMvc
                .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(PHONE, "otra-clave")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(unknownPhone).isEqualTo(wrongPassword)
                .contains("INVALID_CREDENTIALS");
    }

    @Test
    void doesNotApplyTheRegistrationPasswordPolicyOnLogin() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(PHONE, "12345")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAnInvalidPhoneFormat() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("887654321", "clave")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phoneNumber"));
        verifyNoInteractions(authService);
    }

    @Test
    void rejectsAMissingPassword() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + PHONE + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    void allowsPreflightFromTheConfiguredOriginOnly() throws Exception {
        mockMvc.perform(options("/auth/login").header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mockMvc.perform(options("/auth/login").header("Origin", "http://otro.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
