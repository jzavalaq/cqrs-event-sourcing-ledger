package com.ledger.exception;

import com.ledger.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses.
 * <p>
 * Handles all exceptions thrown by controllers and provides standardized
 * error responses in the format: {"error": "message", "status": 400}
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotations.
     *
     * @param ex the MethodArgumentNotValidException
     * @param request the web request
     * @return ErrorResponse with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", message);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error(message)
            .status(HttpStatus.BAD_REQUEST.value())
            .fieldErrors(ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid value"
                )))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles resource not found exceptions.
     *
     * @param ex the ResourceNotFoundException
     * @param request the web request
     * @return ErrorResponse with 404 status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error(ex.getMessage())
            .status(HttpStatus.NOT_FOUND.value())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles insufficient funds exceptions.
     *
     * @param ex the InsufficientFundsException
     * @param request the web request
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex, WebRequest request) {
        log.warn("Insufficient funds: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles invalid operation exceptions.
     *
     * @param ex the InvalidOperationException
     * @param request the web request
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidOperationException ex, WebRequest request) {
        log.warn("Invalid operation: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles bad request exceptions.
     *
     * @param ex the BadRequestException
     * @param request the web request
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles illegal argument exceptions.
     *
     * @param ex the IllegalArgumentException
     * @param request the web request
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles all unhandled exceptions.
     *
     * @param ex the Exception
     * @param request the web request
     * @return ErrorResponse with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred at {}: {}", extractPath(request), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("An unexpected error occurred")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
