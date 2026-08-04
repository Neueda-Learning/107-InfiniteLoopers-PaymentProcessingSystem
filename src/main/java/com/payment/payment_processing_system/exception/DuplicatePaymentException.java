package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when a duplicate payment is detected,
 * typically identified via an idempotency key conflict.
 */
public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String message) {
        super(message);
    }

    public DuplicatePaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}

