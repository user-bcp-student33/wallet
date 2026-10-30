package com.bcp.wallet.backend.auth;

import com.bcp.wallet.backend.account.AccountRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = AccountRules.PHONE_MESSAGE)
        @Pattern(regexp = AccountRules.PHONE_REGEX, message = AccountRules.PHONE_MESSAGE)
        String phoneNumber,

        @NotBlank(message = AccountRules.LOGIN_PASSWORD_MESSAGE)
        String password) {
}
