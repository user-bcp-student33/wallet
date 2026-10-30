package com.bcp.wallet.backend.account;

public enum CardBrand {
    VISA('4'),
    MASTERCARD('5'),
    OTHER('\0');

    private final char leadingDigit;

    CardBrand(char leadingDigit) {
        this.leadingDigit = leadingDigit;
    }

    public static CardBrand fromCardNumber(String cardNumber) {
        char first = cardNumber.charAt(0);
        for (CardBrand brand : values()) {
            if (brand.leadingDigit == first) {
                return brand;
            }
        }
        return OTHER;
    }
}
