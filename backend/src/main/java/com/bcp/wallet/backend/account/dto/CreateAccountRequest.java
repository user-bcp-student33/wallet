package com.bcp.wallet.backend.account.dto;

import com.bcp.wallet.backend.account.AccountRules;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateAccountRequest {

    @NotNull(message = AccountRules.CARD_FORMAT_MESSAGE)
    @Pattern(regexp = AccountRules.CARD_REGEX, message = AccountRules.CARD_FORMAT_MESSAGE)
    private String cardNumber;

    @NotNull(message = AccountRules.CVV_MESSAGE)
    @Pattern(regexp = AccountRules.CVV_REGEX, message = AccountRules.CVV_MESSAGE)
    private String cvv;

    @NotNull(message = AccountRules.PHONE_MESSAGE)
    @Pattern(regexp = AccountRules.PHONE_REGEX, message = AccountRules.PHONE_MESSAGE)
    private String phoneNumber;

    @NotNull(message = AccountRules.PASSWORD_MESSAGE)
    @Size(min = AccountRules.PASSWORD_MIN_LENGTH, max = AccountRules.PASSWORD_MAX_LENGTH,
            message = AccountRules.PASSWORD_MESSAGE)
    private String password;

    @Pattern(regexp = AccountRules.CHANNEL_REGEX, message = AccountRules.CHANNEL_MESSAGE)
    private String acquisitionChannel;

    public CreateAccountRequest() {
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAcquisitionChannel() {
        return acquisitionChannel;
    }

    public void setAcquisitionChannel(String acquisitionChannel) {
        this.acquisitionChannel = acquisitionChannel;
    }
}
