package com.payment.payment_processing_system.dto;

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
    private LocalDateTime transactionTime;
}

