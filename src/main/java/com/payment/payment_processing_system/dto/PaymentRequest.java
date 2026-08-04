package com.payment.payment_processing_system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;

/**
 * DTO for payment request containing payment transaction details.
 * All fields are validated at the API layer before reaching the service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotBlank(message = "Sender account number must not be empty.")
    private String senderAccountNumber;

    @NotBlank(message = "Receiver account number must not be empty.")
    private String receiverAccountNumber;

    @NotBlank(message = "Receiver IFSC code must not be empty.")
    private String receiverIfscCode;

    @NotNull(message = "Transaction amount must not be null.")
    @DecimalMin(value = "0.01", message = "Transaction amount must be greater than zero.")
    @DecimalMax(value = "1000000.00", message = "Transaction amount must not exceed 1,000,000.")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places.")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters.")
    private String description;

    @NotBlank(message = "UPI PIN must not be empty.")
    @Pattern(regexp = "\\d{4}", message = "UPI PIN must contain exactly 4 numeric digits.")
    private String upiPin;
}

