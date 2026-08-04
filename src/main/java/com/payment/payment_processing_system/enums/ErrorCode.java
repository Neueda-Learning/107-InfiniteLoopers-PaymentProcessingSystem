package com.payment.payment_processing_system.enums;

/**
 * Standardized machine-readable error codes for the Payment Processing System.
 * These codes are returned in the API error response body so clients can
 * programmatically handle different failure scenarios.
 */
public enum ErrorCode {

    // 400 - Validation failures
    VALIDATION_FAILED,
    INSUFFICIENT_FUNDS,
    INVALID_ACCOUNT,
    INVALID_AMOUNT,
    INVALID_STATUS_TRANSITION,

    // 401 - Authentication
    INVALID_UPI_PIN,

    // 404 - Not found
    PAYMENT_NOT_FOUND,
    CUSTOMER_NOT_FOUND,
    ACCOUNT_NOT_FOUND,

    // 409 - Conflict
    DUPLICATE_PAYMENT,

    // 429 - Too many requests
    RETRY_LIMIT_EXCEEDED,

    // 500 / 502 - Processing errors
    PAYMENT_FAILED,
    PROCESSING_ERROR,
    NETWORK_ERROR
}

