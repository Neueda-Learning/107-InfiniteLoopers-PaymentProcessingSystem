package com.payment.payment_processing_system.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO for payment request containing payment transaction details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    private String senderAccountNumber;
    private String receiverAccountNumber;
    private String receiverIfscCode;
    private BigDecimal amount;
    private String description;
    private String upiPin;
}

