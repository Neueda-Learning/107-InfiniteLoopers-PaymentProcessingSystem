package com.payment.payment_processing_system.validation;

import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.exception.InvalidPaymentException;
import com.payment.payment_processing_system.exception.InvalidUpiPinException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PaymentValidatorTest {

    private final PaymentValidator paymentValidator = new PaymentValidator();

    @Test
    @DisplayName("Valid payment request should pass validation")
    void validate_whenRequestIsValid_shouldPass() {
        assertDoesNotThrow(() -> paymentValidator.validate(validRequest()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Blank sender account should be rejected")
    void validate_whenSenderAccountIsBlank_shouldThrowInvalidPaymentException(String senderAccountNumber) {
        PaymentRequest request = baseRequestBuilder()
                .senderAccountNumber(senderAccountNumber)
                .build();

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () -> paymentValidator.validate(request));
        assertEquals("Sender account number must not be empty.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Blank receiver account should be rejected")
    void validate_whenReceiverAccountIsBlank_shouldThrowInvalidPaymentException(String receiverAccountNumber) {
        PaymentRequest request = baseRequestBuilder()
                .receiverAccountNumber(receiverAccountNumber)
                .build();

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () -> paymentValidator.validate(request));
        assertEquals("Receiver account number must not be empty.", ex.getMessage());
    }

    @Test
    @DisplayName("Same sender and receiver account should be rejected")
    void validate_whenSenderAndReceiverAreSame_shouldThrowInvalidPaymentException() {
        PaymentRequest request = baseRequestBuilder()
                .receiverAccountNumber("100000000001")
                .build();

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () -> paymentValidator.validate(request));
        assertEquals("Sender and receiver account numbers cannot be the same.", ex.getMessage());
    }

    @Test
    @DisplayName("Null amount should be rejected")
    void validate_whenAmountIsNull_shouldThrowInvalidPaymentException() {
        PaymentRequest request = baseRequestBuilder()
                .amount(null)
                .build();

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () -> paymentValidator.validate(request));
        assertEquals("Transaction amount must not be null.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-10.25"})
    @DisplayName("Zero or negative amount should be rejected")
    void validate_whenAmountIsNotPositive_shouldThrowInvalidPaymentException(String amount) {
        PaymentRequest request = baseRequestBuilder()
                .amount(new BigDecimal(amount))
                .build();

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () -> paymentValidator.validate(request));
        assertEquals("Transaction amount must be greater than zero. Provided: " + new BigDecimal(amount).toPlainString(), ex.getMessage());
    }

    @Test
    @DisplayName("Description longer than 255 characters should be rejected")
    void validate_whenDescriptionTooLong_shouldThrowInvalidPaymentException() {
        String longDescription = "a".repeat(256);
        PaymentRequest request = baseRequestBuilder()
                .description(longDescription)
                .build();

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () -> paymentValidator.validate(request));
        assertEquals("Description must not exceed 255 characters. Provided length: 256", ex.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Blank UPI PIN should be rejected")
    void validate_whenUpiPinIsBlank_shouldThrowInvalidUpiPinException(String upiPin) {
        PaymentRequest request = baseRequestBuilder()
                .upiPin(upiPin)
                .build();

        InvalidUpiPinException ex = assertThrows(InvalidUpiPinException.class, () -> paymentValidator.validate(request));
        assertEquals("UPI PIN must not be empty.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12a4", "12345", "abcd"})
    @DisplayName("Malformed UPI PIN should be rejected")
    void validate_whenUpiPinIsMalformed_shouldThrowInvalidUpiPinException(String upiPin) {
        PaymentRequest request = baseRequestBuilder()
                .upiPin(upiPin)
                .build();

        InvalidUpiPinException ex = assertThrows(InvalidUpiPinException.class, () -> paymentValidator.validate(request));
        assertEquals("UPI PIN must contain exactly 4 numeric digits.", ex.getMessage());
    }

    private PaymentRequest.PaymentRequestBuilder baseRequestBuilder() {
        return PaymentRequest.builder()
                .senderAccountNumber("100000000001")
                .receiverAccountNumber("100000000002")
                .receiverIfscCode("HDFC0005678")
                .amount(new BigDecimal("100.00"))
                .description("Test payment")
                .upiPin("1234");
    }

    private PaymentRequest validRequest() {
        return baseRequestBuilder().build();
    }
}



