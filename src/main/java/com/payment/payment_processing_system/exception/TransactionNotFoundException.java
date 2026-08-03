package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when a transaction is not found in the system.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

