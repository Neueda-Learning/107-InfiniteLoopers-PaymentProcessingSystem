package com.payment.payment_processing_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for exposing safe account details.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private String accountNumber;
    private String bankName;
    private String ifscCode;
    private BigDecimal balance;
    private String customerName;
    private String email;
    private String currency;
}

