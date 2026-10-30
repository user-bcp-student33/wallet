package com.bcp.wallet.backend.error;

public class AccessForbiddenException extends RuntimeException {

    public AccessForbiddenException() {
        super("Access denied");
    }
}
