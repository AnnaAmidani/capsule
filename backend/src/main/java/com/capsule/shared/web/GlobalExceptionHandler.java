package com.capsule.shared.web;

import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason() != null
                ? ex.getReason()
                : (ex.getStatusCode() instanceof HttpStatus hs ? hs.getReasonPhrase() : "Error");
        var error = ApiError.of(ex.getStatusCode().value(), reason, ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining(", "));
        return ResponseEntity.status(400)
                .body(ApiError.of(400, "Validation Failed", message));
    }

    // Note: Spring Security intercepts AccessDeniedException via ExceptionTranslationFilter
    // before it reaches this advice. This handler only fires for AccessDeniedException thrown
    // directly inside @Controller methods. Security-layer 403 responses are configured via
    // SecurityConfig.exceptionHandling().accessDeniedHandler(...) in Task 5.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN.value())
                .body(ApiError.of(HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(500)
                .body(ApiError.of(500, "Internal Server Error", "An unexpected error occurred"));
    }
}
