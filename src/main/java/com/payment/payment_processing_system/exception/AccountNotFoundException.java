package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when an account is not found in the system.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }

    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

