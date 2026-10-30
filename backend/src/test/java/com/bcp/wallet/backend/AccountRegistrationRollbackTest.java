package com.bcp.wallet.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bcp.wallet.backend.account.LinkedCard;
import com.bcp.wallet.backend.account.LinkedCardRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AccountRegistrationRollbackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private LinkedCardRepository cards;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("delete from onboarding_events");
        jdbc.update("delete from linked_cards");
        jdbc.update("delete from acquisition_spend");
        jdbc.update("delete from accounts");
    }

    @Test
    void rollsBackAccountAndOnboardingEventsWhenCardLinkingFails() throws Exception {
        when(cards.save(any(LinkedCard.class))).thenThrow(new IllegalStateException("card persistence failed"));
        String request = """
                {"cardNumber":"4111111111111111","cvv":"123","phoneNumber":"987654321","password":"clave-segura-1"}
                """;

        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from accounts", Long.class))
                .isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from linked_cards", Long.class))
                .isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from onboarding_events", Long.class))
                .isZero();
    }
}
