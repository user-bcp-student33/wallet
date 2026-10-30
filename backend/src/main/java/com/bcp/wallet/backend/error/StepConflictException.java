package com.bcp.wallet.backend.error;

public class StepConflictException extends RuntimeException {

    public StepConflictException() {
        super("Step is not the current onboarding step");
    }
}
