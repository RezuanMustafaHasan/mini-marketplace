package com.hasan.marketplace.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.hasan.marketplace.controller.api")
public class ApiGlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex,
                                                                       HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler({UnauthorizedActionException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleForbidden(RuntimeException ex,
                                                               HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            InvalidCategoryException.class,
            InsufficientStockException.class,
            InvalidOrderStateTransitionException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex,
                                                                HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex,
                                                                         HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(error.getField(), error.getDefaultMessage());
        }

        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                validationErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request,
                null);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status,
                                                                    String message,
                                                                    HttpServletRequest request,
                                                                    Map<String, String> validationErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());

        if (validationErrors != null && !validationErrors.isEmpty()) {
            body.put("validationErrors", validationErrors);
        }

        return ResponseEntity.status(status).body(body);
    }
}
