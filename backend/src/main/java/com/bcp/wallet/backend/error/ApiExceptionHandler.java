package com.bcp.wallet.backend.error;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bcp.wallet.backend.error.ApiError.FieldProblem;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDATION_CODE = "VALIDATION_ERROR";
    static final String VALIDATION_MESSAGE = "Revisa los datos ingresados.";
    static final String MALFORMED_BODY_MESSAGE = "El cuerpo de la solicitud no es válido.";
    static final String INVALID_PARAMETER_MESSAGE = "El valor indicado no es válido.";
    static final String DUPLICATE_CODE = "ACCOUNT_CONFLICT";
    static final String DUPLICATE_MESSAGE = "Este número de celular ya está en uso.";
    static final String STEP_CODE = "ONBOARDING_STEP_CONFLICT";
    static final String STEP_MESSAGE = "El paso indicado no corresponde al paso actual del onboarding.";
    static final String NOT_FOUND_CODE = "ACCOUNT_NOT_FOUND";
    static final String NOT_FOUND_MESSAGE = "No encontramos la cuenta indicada.";
    static final String CREDENTIALS_CODE = "INVALID_CREDENTIALS";
    static final String CREDENTIALS_MESSAGE = "Celular o contraseña incorrectos.";
    static final String FORBIDDEN_CODE = "ACCESS_DENIED";
    static final String FORBIDDEN_MESSAGE = "Acceso denegado.";
    static final String INTERNAL_CODE = "INTERNAL_ERROR";
    static final String INTERNAL_MESSAGE = "Ocurrió un error inesperado. Inténtalo nuevamente.";

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> messagesByField = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> messagesByField.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return validationFailure(messagesByField.entrySet().stream()
                .map(entry -> new FieldProblem(entry.getKey(), entry.getValue()))
                .toList());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return body(HttpStatus.BAD_REQUEST, ApiError.of(VALIDATION_CODE, MALFORMED_BODY_MESSAGE));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return validationFailure(List.of(new FieldProblem(ex.getPropertyName(), INVALID_PARAMETER_MESSAGE)));
    }

    @ExceptionHandler(InvalidFieldException.class)
    ResponseEntity<Object> handleInvalidField(InvalidFieldException ex) {
        return validationFailure(List.of(new FieldProblem(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(DuplicatePhoneException.class)
    ResponseEntity<Object> handleDuplicatePhone() {
        return body(HttpStatus.CONFLICT, ApiError.of(DUPLICATE_CODE, DUPLICATE_MESSAGE));
    }

    @ExceptionHandler(StepConflictException.class)
    ResponseEntity<Object> handleStepConflict() {
        return body(HttpStatus.CONFLICT, ApiError.of(STEP_CODE, STEP_MESSAGE));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<Object> handleAccountNotFound() {
        return body(HttpStatus.NOT_FOUND, ApiError.of(NOT_FOUND_CODE, NOT_FOUND_MESSAGE));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<Object> handleInvalidCredentials() {
        return body(HttpStatus.UNAUTHORIZED, ApiError.of(CREDENTIALS_CODE, CREDENTIALS_MESSAGE));
    }

    @ExceptionHandler(AccessForbiddenException.class)
    ResponseEntity<Object> handleForbidden() {
        return body(HttpStatus.FORBIDDEN, ApiError.of(FORBIDDEN_CODE, FORBIDDEN_MESSAGE));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex) {
        LOG.error("Unexpected error: {}", ex.getClass().getName(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ApiError.of(INTERNAL_CODE, INTERNAL_MESSAGE));
    }

    private ResponseEntity<Object> validationFailure(List<FieldProblem> problems) {
        List<FieldProblem> sorted = problems.stream().sorted(Comparator.comparing(FieldProblem::field)).toList();
        return body(HttpStatus.BAD_REQUEST, new ApiError(VALIDATION_CODE, VALIDATION_MESSAGE, sorted));
    }

    private static ResponseEntity<Object> body(HttpStatus status, ApiError error) {
        return ResponseEntity.status(status).body(error);
    }
}
