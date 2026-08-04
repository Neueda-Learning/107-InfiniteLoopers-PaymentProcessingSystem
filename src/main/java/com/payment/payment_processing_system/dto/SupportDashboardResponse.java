package com.payment.payment_processing_system.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO for support dashboard response containing system statistics and metrics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportDashboardResponse {

    private Long totalCustomers;
    private Long totalTransactions;
    private Long successfulTransactions;
    private Long failedTransactions;
    private BigDecimal totalCreditAmount;
    private BigDecimal totalDebitAmount;
}

