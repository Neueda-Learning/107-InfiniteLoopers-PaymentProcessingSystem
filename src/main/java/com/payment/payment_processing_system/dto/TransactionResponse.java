package com.payment.payment_processing_system.dto;

import com.payment.payment_processing_system.enums.CurrencyType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transaction response containing full transaction details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private String transactionId;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private CurrencyType senderCurrency;
    private CurrencyType receiverCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal transferCharge;
    private BigDecimal convertedAmount;
    private String description;
    private String paymentStatus;
    private LocalDateTime createdTime;
    private LocalDateTime validatedTime;
    private LocalDateTime sentTime;
    private LocalDateTime completedTime;
    private LocalDateTime failedTime;
}

