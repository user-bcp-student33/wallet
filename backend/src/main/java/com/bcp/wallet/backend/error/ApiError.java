package com.bcp.wallet.backend.error;

import java.util.List;

public record ApiError(String code, String message, List<FieldProblem> fieldErrors) {

    public record FieldProblem(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
