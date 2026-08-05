package com.payment.payment_processing_system.dto;

import com.payment.payment_processing_system.enums.CurrencyType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for payment response containing transaction confirmation details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private String transactionId;
    private String paymentStatus;
    private String message;
    private boolean idempotentReplay;
    private BigDecimal amount;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private CurrencyType senderCurrency;
    private CurrencyType receiverCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal transferCharge;
    private BigDecimal convertedAmount;
    private BigDecimal totalDeducted;
    private boolean conversionRequired;
    private LocalDateTime transactionTime;
}

