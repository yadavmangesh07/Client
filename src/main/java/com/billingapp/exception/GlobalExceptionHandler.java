package com.billingapp.exception;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MDC_CORRELATION_KEY = "CorrelationId";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        String currentTraceId = MDC.get(MDC_CORRELATION_KEY);
        log.warn("Business validation rule triggered: {} [TraceID: {}]", ex.getMessage(), currentTraceId);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        body.put("traceId", currentTraceId); // Added trace context mapping
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String currentTraceId = MDC.get(MDC_CORRELATION_KEY);
        log.warn("Input model validation failed for incoming payload [TraceID: {}]", currentTraceId);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("traceId", currentTraceId); // Added trace context mapping

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        body.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        String currentTraceId = MDC.get(MDC_CORRELATION_KEY);
        
        // This will dump the standard stack trace tied to the active TraceID row inside logback
        log.error("CRITICAL UNHANDLED EXCEPTION CAUGHT [TraceID: " + currentTraceId + "]: " + ex.getMessage(), ex);

        // Retain original Sentry connection
        Sentry.captureException(ex);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Internal server error");
        body.put("traceId", currentTraceId); // Added trace context mapping
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}