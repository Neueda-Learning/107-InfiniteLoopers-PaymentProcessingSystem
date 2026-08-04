package com.payment.payment_processing_system.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO for customer response containing customer and account information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private Long customerId;
    private String customerName;
    private String email;
    private String phoneNumber;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private BigDecimal balance;
}

