package com.payment.payment_processing_system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for payment preview operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviewPaymentRequest {

    @NotBlank(message = "Sender account number must not be empty.")
    private String senderAccountNumber;

    @NotBlank(message = "Receiver account number must not be empty.")
    private String receiverAccountNumber;

    @NotNull(message = "Transaction amount must not be null.")
    @DecimalMin(value = "0.01", message = "Transaction amount must be greater than zero.")
    @DecimalMax(value = "1000000.00", message = "Transaction amount must not exceed 1,000,000.")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places.")
    private BigDecimal amount;
}

