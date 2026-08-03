package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when a payment request contains invalid data,
 * such as a zero or negative amount, same sender and receiver account,
 * or a missing required field.
 */
public class InvalidPaymentException extends RuntimeException {

    public InvalidPaymentException(String message) {
        super(message);
    }

    public InvalidPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}

