package com.payment.payment_processing_system.validation;

import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.exception.InvalidPaymentException;
import com.payment.payment_processing_system.exception.InvalidUpiPinException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stateless validator for PaymentRequest objects.
 * Performs all field-level validations before the request reaches the service layer.
 * Does not access the database.
 */
@Slf4j
@Component
public class PaymentValidator {

    private static final int MAX_DESCRIPTION_LENGTH = 255;
    private static final int REQUIRED_UPI_PIN_LENGTH = 4;
    private static final String UPI_PIN_PATTERN = "\\d{4}";

    /**
     * Validates the given {@link PaymentRequest} against all business rules.
     * Throws a specific exception immediately upon the first violation found.
     *
     * @param request the payment request to validate
     * @throws InvalidPaymentException  if any payment field is invalid
     * @throws InvalidUpiPinException   if the UPI PIN is missing or not exactly 4 digits
     */
    public void validate(PaymentRequest request) {
        log.debug("Validating payment request from sender: {}", request.getSenderAccountNumber());

        validateSenderAccountNumber(request.getSenderAccountNumber());
        validateReceiverAccountNumber(request.getReceiverAccountNumber());
        validateSenderReceiverNotSame(request.getSenderAccountNumber(), request.getReceiverAccountNumber());
        validateAmount(request.getAmount());
        validateDescription(request.getDescription());
        validateUpiPin(request.getUpiPin());

        log.debug("Payment request validation passed for sender: {}", request.getSenderAccountNumber());
    }

    // ─── Individual Validators ────────────────────────────────────────────────

    /**
     * Rule 1: Sender account number must not be null or blank.
     */
    private void validateSenderAccountNumber(String senderAccountNumber) {
        if (senderAccountNumber == null || senderAccountNumber.isBlank()) {
            throw new InvalidPaymentException("Sender account number must not be empty.");
        }
    }

    /**
     * Rule 2: Receiver account number must not be null or blank.
     */
    private void validateReceiverAccountNumber(String receiverAccountNumber) {
        if (receiverAccountNumber == null || receiverAccountNumber.isBlank()) {
            throw new InvalidPaymentException("Receiver account number must not be empty.");
        }
    }

    /**
     * Rule 3: Sender and receiver account numbers must be different.
     */
    private void validateSenderReceiverNotSame(String senderAccountNumber, String receiverAccountNumber) {
        if (senderAccountNumber != null
                && senderAccountNumber.equalsIgnoreCase(receiverAccountNumber)) {
            throw new InvalidPaymentException(
                    "Sender and receiver account numbers cannot be the same.");
        }
    }

    /**
     * Rule 4: Transaction amount must be a positive value greater than zero.
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidPaymentException("Transaction amount must not be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException(
                    "Transaction amount must be greater than zero. Provided: " + amount.toPlainString());
        }
    }

    /**
     * Rule 5: Description, if provided, must not exceed 255 characters.
     */
    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidPaymentException(
                    "Description must not exceed " + MAX_DESCRIPTION_LENGTH + " characters. "
                    + "Provided length: " + description.length());
        }
    }

    /**
     * Rule 6: UPI PIN must not be null or blank.
     * Rule 7: UPI PIN must contain exactly 4 numeric digits.
     */
    private void validateUpiPin(String upiPin) {
        if (upiPin == null || upiPin.isBlank()) {
            throw new InvalidUpiPinException("UPI PIN must not be empty.");
        }
        if (!upiPin.matches(UPI_PIN_PATTERN)) {
            throw new InvalidUpiPinException(
                    "UPI PIN must contain exactly " + REQUIRED_UPI_PIN_LENGTH + " numeric digits.");
        }
    }
}

