package com.payment.payment_processing_system.exception;

/**
 * Exception thrown when an exchange rate is not configured for a currency pair.
 */
public class UnsupportedExchangeRateException extends RuntimeException {

    public UnsupportedExchangeRateException(String message) {
        super(message);
    }
}

