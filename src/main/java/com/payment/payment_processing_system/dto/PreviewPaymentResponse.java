package com.payment.payment_processing_system.dto;

import com.payment.payment_processing_system.enums.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Response DTO for payment preview operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviewPaymentResponse {

	private CurrencyType senderCurrency;
	private CurrencyType receiverCurrency;
	private BigDecimal exchangeRate;
	private BigDecimal originalAmount;
	private BigDecimal convertedAmount;
	private BigDecimal transferCharge;
	private BigDecimal totalDeducted;
	private boolean conversionRequired;
}

