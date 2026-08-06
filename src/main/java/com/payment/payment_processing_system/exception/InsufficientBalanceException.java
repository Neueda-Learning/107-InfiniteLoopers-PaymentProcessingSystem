package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when the sender's account balance is insufficient
 * to complete the requested payment transaction.
 */
public class InsufficientBalanceException extends RuntimeException {

    private final String transactionId;

    public InsufficientBalanceException(String message) {
        super(message);
        this.transactionId = null;
    }

    public InsufficientBalanceException(String message, String transactionId) {
        super(message);
        this.transactionId = transactionId;
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
        this.transactionId = null;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
