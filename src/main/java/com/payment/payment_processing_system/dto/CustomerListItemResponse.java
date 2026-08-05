package com.payment.payment_processing_system.dto;

/**
 * DTO used for customer selection lists.
 */
public record CustomerListItemResponse(
        Long id,
        String customerName
) {
}

