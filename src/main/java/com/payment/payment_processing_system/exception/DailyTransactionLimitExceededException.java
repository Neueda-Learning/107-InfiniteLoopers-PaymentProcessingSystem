package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when a payment transaction exceeds the daily transaction limit for an account.
 */
public class DailyTransactionLimitExceededException extends RuntimeException {

    private final String transactionId;

    public DailyTransactionLimitExceededException(String message) {
        super(message);
        this.transactionId = null;
    }

    public DailyTransactionLimitExceededException(String message, String transactionId) {
        super(message);
        this.transactionId = transactionId;
    }

    public DailyTransactionLimitExceededException(String message, Throwable cause) {
        super(message, cause);
        this.transactionId = null;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
