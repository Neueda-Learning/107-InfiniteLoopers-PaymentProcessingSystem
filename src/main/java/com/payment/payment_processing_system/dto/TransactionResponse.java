package com.payment.payment_processing_system.dto;

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
    private String description;
    private String paymentStatus;
    private LocalDateTime createdTime;
    private LocalDateTime validatedTime;
    private LocalDateTime sentTime;
    private LocalDateTime completedTime;
    private LocalDateTime failedTime;
}

