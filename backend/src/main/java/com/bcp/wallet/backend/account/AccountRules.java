package com.bcp.wallet.backend.account;

public final class AccountRules {

    public static final String PHONE_REGEX = "^9\\d{8}$";
    public static final String PHONE_MESSAGE = "Ingresa un celular peruano válido de 9 dígitos que empiece con 9.";

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;
    public static final String PASSWORD_MESSAGE = "La contraseña debe tener entre 8 y 128 caracteres.";
    public static final String LOGIN_PASSWORD_MESSAGE = "Ingresa tu contraseña.";

    public static final String CARD_REGEX = "^\\d{1,16}$";
    public static final String CARD_FORMAT_MESSAGE = "Ingresa el número de tarjeta: solo números, hasta 16 dígitos.";
    public static final int CARD_LAST_DIGITS = 4;

    public static final String CVV_REGEX = "^\\d{3}$";
    public static final String CVV_MESSAGE = "Ingresa un CVV de 3 dígitos.";

    public static final String CHANNEL_REGEX = "^[a-z0-9_-]{1,40}$";
    public static final String CHANNEL_MESSAGE = "El canal de adquisición solo admite minúsculas, dígitos, guion y guion bajo (hasta 40 caracteres).";
    public static final String DEFAULT_CHANNEL = "direct";

    private AccountRules() {
    }
}
