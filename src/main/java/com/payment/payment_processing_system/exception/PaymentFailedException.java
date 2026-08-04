package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when a payment transaction fails during processing
 * due to a system-level or business-level error.
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

