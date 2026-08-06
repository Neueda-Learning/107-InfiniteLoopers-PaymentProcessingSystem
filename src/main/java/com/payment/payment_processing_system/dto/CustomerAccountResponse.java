package com.payment.payment_processing_system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.payment_processing_system.enums.CurrencyType;

import java.math.BigDecimal;

/**
 * DTO used for customer account selection.
 */
public record CustomerAccountResponse(
        Long accountId,
        String accountNumber,
        String bankName,
        String ifscCode,
        BigDecimal balance,
        CurrencyType currency,
        @JsonProperty("isActive") boolean isActive
) {
}

