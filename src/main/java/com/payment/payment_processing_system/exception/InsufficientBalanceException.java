package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when the sender's account balance is insufficient
 * to complete the requested payment transaction.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}

