package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when a payment transaction exceeds the daily transaction limit for an account.
 */
public class DailyTransactionLimitExceededException extends RuntimeException {

    public DailyTransactionLimitExceededException(String message) {
        super(message);
    }

    public DailyTransactionLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}

