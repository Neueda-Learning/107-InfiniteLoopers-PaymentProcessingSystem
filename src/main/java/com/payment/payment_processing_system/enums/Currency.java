package com.payment.payment_processing_system.enums;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Enum for supported currencies and their daily transaction limits.
 * Each currency has a default daily limit that can be used for account restrictions.
 */
@Getter
public enum Currency {
    INR("Indian Rupee", new BigDecimal("100000.00")),      // 1,00,000 INR
    USD("US Dollar", new BigDecimal("1200.00")),           // $1200 USD (approximately)
    EUR("Euro", new BigDecimal("1000.00")),                // €1000 EUR (approximately)
    GBP("British Pound", new BigDecimal("950.00"));        // £950 GBP (approximately)

    private final String description;
    private final BigDecimal defaultDailyLimit;

    Currency(String description, BigDecimal defaultDailyLimit) {
        this.description = description;
        this.defaultDailyLimit = defaultDailyLimit;
    }
}

