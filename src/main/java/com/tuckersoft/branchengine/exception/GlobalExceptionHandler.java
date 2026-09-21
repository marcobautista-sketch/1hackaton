package com.tuckersoft.branchengine.exception;

import com.tuckersoft.branchengine.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centraliza el formato de error del enunciado: { error, message, timestamp, path }.
 * Los 401/403 de Spring Security NO pasan por aqui: los maneja el
 * AuthenticationEntryPoint y el AccessDeniedHandler, registrados en SecurityConfig.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Datos invalidos en el request.";
        }
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Error inesperado: " + ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message,
                                                 HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(error, message, Instant.now(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
