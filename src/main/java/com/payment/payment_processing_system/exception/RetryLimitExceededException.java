package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when the maximum number of retry attempts
 * has been reached for a failed payment transaction.
 */
public class RetryLimitExceededException extends RuntimeException {

    public RetryLimitExceededException(String message) {
        super(message);
    }

    public RetryLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}

