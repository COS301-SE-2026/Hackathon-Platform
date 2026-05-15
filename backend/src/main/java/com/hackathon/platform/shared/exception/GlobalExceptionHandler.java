package com.hackathon.platform.shared.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationErrors(
      MethodArgumentNotValidException ex) {

    Map<String, String> fieldErrors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(error.getField(), error.getDefaultMessage());
    }

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            Map.of(
                "status",
                400,
                "error",
                "Validation failed",
                "errors",
                fieldErrors,
                "timestamp",
                LocalDateTime.now().toString()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            Map.of(
                "status", 401,
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "status", 409,
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()));
  }

  @ExceptionHandler(UsernameNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleUserNotFound(UsernameNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "status", 404,
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {

    ex.printStackTrace();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            Map.of(
                "status", 500,
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()));
  }
}
