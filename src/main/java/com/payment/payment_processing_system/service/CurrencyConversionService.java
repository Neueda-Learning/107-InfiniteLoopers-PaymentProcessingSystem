package com.payment.payment_processing_system.service;

import com.payment.payment_processing_system.enums.CurrencyType;

import java.math.BigDecimal;

/**
 * Service for currency exchange and transfer charge calculations.
 */
public interface CurrencyConversionService {

    /**
     * Returns the exchange rate between two currencies.
     *
     * @param from source currency
     * @param to target currency
     * @return exchange rate from source to target currency
     */
    BigDecimal getExchangeRate(CurrencyType from, CurrencyType to);

    /**
     * Converts an amount from one currency to another.
     *
     * @param amount source amount
     * @param from source currency
     * @param to target currency
     * @return converted amount
     */
    BigDecimal convertAmount(BigDecimal amount, CurrencyType from, CurrencyType to);

    /**
     * Calculates the transfer charge for a transaction.
     *
     * @param amount sender amount
     * @param from source currency
     * @param to target currency
     * @return transfer charge amount
     */
    BigDecimal calculateTransferCharge(BigDecimal amount, CurrencyType from, CurrencyType to);
}

