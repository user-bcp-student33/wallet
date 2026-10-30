package com.bcp.wallet.backend.onboarding;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bcp.wallet.backend.account.AccountStatus;
import com.bcp.wallet.backend.error.AccountNotFoundException;
import com.bcp.wallet.backend.error.StepConflictException;

@WebMvcTest(OnboardingController.class)
class OnboardingControllerTest {

    private final UUID accountId = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnboardingService onboarding;

    @Test
    void returnsTheStepSequenceAndCurrentStep() throws Exception {
        when(onboarding.stateOf(accountId)).thenReturn(new OnboardingStateResponse(accountId,
                AccountStatus.PENDING_ONBOARDING, 1, 3, OnboardingStep.WALLET_PREPARATION));

        mockMvc.perform(get("/accounts/{id}/onboarding", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_ONBOARDING"))
                .andExpect(jsonPath("$.completedSteps").value(1))
                .andExpect(jsonPath("$.totalSteps").value(3))
                .andExpect(jsonPath("$.currentStep").value("WALLET_PREPARATION"));
    }

    @Test
    void returnsNotFoundWithAGenericMessage() throws Exception {
        when(onboarding.stateOf(accountId)).thenThrow(new AccountNotFoundException());

        mockMvc.perform(get("/accounts/{id}/onboarding", accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void activatesTheAccountWhenTheLastStepIsCompleted() throws Exception {
        when(onboarding.complete(accountId, OnboardingStep.WALLET_READY)).thenReturn(
                new OnboardingStateResponse(accountId, AccountStatus.ACTIVE, 3, 3, null));

        mockMvc.perform(post("/accounts/{id}/onboarding/steps/WALLET_READY/complete", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.completedSteps").value(3))
                .andExpect(jsonPath("$.currentStep").doesNotExist());
    }

    @Test
    void returnsConflictWhenTheStepIsNotTheCurrentOne() throws Exception {
        when(onboarding.complete(accountId, OnboardingStep.WALLET_READY)).thenThrow(new StepConflictException());

        mockMvc.perform(post("/accounts/{id}/onboarding/steps/WALLET_READY/complete", accountId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ONBOARDING_STEP_CONFLICT"));
    }

    @Test
    void rejectsAnUnknownStep() throws Exception {
        mockMvc.perform(post("/accounts/{id}/onboarding/steps/INVENTED/complete", accountId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("step"));
    }

    @Test
    void rejectsAMalformedAccountId() throws Exception {
        mockMvc.perform(get("/accounts/{id}/onboarding", "no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("accountId"));
    }
}
