package com.payment.payment_processing_system.exception;

import com.payment.payment_processing_system.dto.ErrorResponse;
import com.payment.payment_processing_system.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Payment Processing System.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFoundException(
            CustomerNotFoundException ex, HttpServletRequest request) {
        log.error("Customer not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.CUSTOMER_NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(
            AccountNotFoundException ex, HttpServletRequest request) {
        log.error("Account not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.ACCOUNT_NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFoundException(
            TransactionNotFoundException ex, HttpServletRequest request) {
        log.error("Transaction not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.PAYMENT_NOT_FOUND, ex.getMessage(), request);
    }

    // ─── 400 Bad Request ──────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error("Bean validation failed: {}", fieldErrors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, fieldErrors, request);
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentException(
            InvalidPaymentException ex, HttpServletRequest request) {
        log.error("Invalid payment request: {}", ex.getMessage());
        // Distinguish transition errors from general validation errors for correct error code
        ErrorCode code = ex.getMessage() != null && ex.getMessage().contains("Invalid status transition")
                ? ErrorCode.INVALID_STATUS_TRANSITION
                : ErrorCode.VALIDATION_FAILED;
        return buildErrorResponse(HttpStatus.BAD_REQUEST, code, ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentValidationException(
            PaymentValidationException ex, HttpServletRequest request) {
        log.error("Payment validation failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, ex.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining("; "));
        log.error("Constraint validation failed: {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        log.error("Path/query parameter type mismatch: {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Invalid request argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedExchangeRateException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedExchangeRateException(
            UnsupportedExchangeRateException ex, HttpServletRequest request) {
        log.error("Unsupported exchange rate: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ErrorCode.UNSUPPORTED_EXCHANGE_RATE, ex.getMessage(), request);
    }

    // ─── 401 Unauthorized ─────────────────────────────────────────────────────

    @ExceptionHandler(InvalidUpiPinException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUpiPinException(
            InvalidUpiPinException ex, HttpServletRequest request) {
        log.error("Invalid UPI PIN: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_UPI_PIN, ex.getMessage(), request);
    }

    // ─── 409 Conflict ─────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePaymentException(
            DuplicatePaymentException ex, HttpServletRequest request) {
        log.error("Duplicate payment detected: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_PAYMENT, ex.getMessage(), request);
    }

    // ─── 422 Unprocessable Entity ─────────────────────────────────────────────

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(
            InsufficientBalanceException ex, HttpServletRequest request) {
        log.error("Insufficient balance: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INSUFFICIENT_FUNDS, ex.getMessage(), request);
    }

    @ExceptionHandler(DailyTransactionLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDailyTransactionLimitExceededException(
            DailyTransactionLimitExceededException ex, HttpServletRequest request) {
        log.error("Daily transaction limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.DAILY_TRANSACTION_LIMIT_EXCEEDED, ex.getMessage(), request);
    }

    // ─── 429 Too Many Requests ────────────────────────────────────────────────

    @ExceptionHandler(RetryLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRetryLimitExceededException(
            RetryLimitExceededException ex, HttpServletRequest request) {
        log.error("Retry limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RETRY_LIMIT_EXCEEDED, ex.getMessage(), request);
    }

    @ExceptionHandler(MaxRetryExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxRetryExceededException(
            MaxRetryExceededException ex, HttpServletRequest request) {
        log.error("Max retry limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RETRY_LIMIT_EXCEEDED, ex.getMessage(), request);
    }

    // ─── 502 Bad Gateway ──────────────────────────────────────────────────────

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailedException(
            PaymentFailedException ex, HttpServletRequest request) {
        log.error("Payment processing failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, ErrorCode.PAYMENT_FAILED, ex.getMessage(), request);
    }

    // ─── 500 Internal Server Error ────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.PROCESSING_ERROR,
                "An unexpected error occurred. Please try again later.", request);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, ErrorCode errorCode, String message, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}
