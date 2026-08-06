package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when the UPI PIN provided by the sender
 * does not match the PIN registered for the account.
 */
public class InvalidUpiPinException extends RuntimeException {

    private final String transactionId;

    public InvalidUpiPinException(String message) {
        super(message);
        this.transactionId = null;
    }

    public InvalidUpiPinException(String message, String transactionId) {
        super(message);
        this.transactionId = transactionId;
    }

    public InvalidUpiPinException(String message, Throwable cause) {
        super(message, cause);
        this.transactionId = null;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
