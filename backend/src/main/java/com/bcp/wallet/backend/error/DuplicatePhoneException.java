package com.bcp.wallet.backend.error;

public class DuplicatePhoneException extends RuntimeException {

    public DuplicatePhoneException() {
        super("Phone number already registered");
    }
}
