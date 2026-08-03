package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when the maximum retry limit for a payment transaction has been reached.
 */
public class MaxRetryExceededException extends RuntimeException {

    public MaxRetryExceededException(String message) {
        super(message);
    }

    public MaxRetryExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}

