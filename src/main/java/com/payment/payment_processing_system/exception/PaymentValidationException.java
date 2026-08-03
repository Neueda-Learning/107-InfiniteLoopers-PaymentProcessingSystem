package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when payment validation fails (e.g. insufficient balance, wrong UPI PIN).
 */
public class PaymentValidationException extends RuntimeException {

    public PaymentValidationException(String message) {
        super(message);
    }

    public PaymentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

