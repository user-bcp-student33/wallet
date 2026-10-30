package com.bcp.wallet.backend.account;

public record CreateAccountCommand(String cardNumber, String phoneNumber, String password, String acquisitionChannel) {
}
