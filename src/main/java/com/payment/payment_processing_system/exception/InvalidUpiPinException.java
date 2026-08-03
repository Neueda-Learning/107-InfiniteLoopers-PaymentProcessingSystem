package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when the UPI PIN provided by the sender
 * does not match the PIN registered for the account.
 */
public class InvalidUpiPinException extends RuntimeException {

    public InvalidUpiPinException(String message) {
        super(message);
    }

    public InvalidUpiPinException(String message, Throwable cause) {
        super(message, cause);
    }
}

