package com.payment.payment_processing_system.validation;

import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.InvalidPaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatusTransitionValidatorTest {

    private final StatusTransitionValidator validator = new StatusTransitionValidator();

    @ParameterizedTest(name = "{0} -> {1} should be valid")
    @MethodSource("validTransitions")
    @DisplayName("Allowed status transitions should pass validation")
    void validate_whenTransitionIsAllowed_shouldPass(PaymentStatus current, PaymentStatus target) {
        assertDoesNotThrow(() -> validator.validate("TXN-001", current, target));
    }

    @ParameterizedTest(name = "{0} -> {1} should be invalid")
    @MethodSource("invalidTransitions")
    @DisplayName("Disallowed status transitions should be rejected")
    void validate_whenTransitionIsNotAllowed_shouldThrowInvalidPaymentException(PaymentStatus current, PaymentStatus target) {
        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class,
                () -> validator.validate("TXN-002", current, target));

        assertEquals(
                "Invalid status transition for transaction [TXN-002]: " + current + " → " + target + " is not allowed. " +
                        "Allowed transitions from " + current + ": " +
                        (current == PaymentStatus.COMPLETED ? "none (terminal state)" : expectedAllowedTransitions(current)),
                ex.getMessage());
    }

    @Test
    @DisplayName("Completed transaction should have no allowed next transitions")
    void validate_whenCompletedToAnyOtherStatus_shouldThrowInvalidPaymentException() {
        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class,
                () -> validator.validate("TXN-003", PaymentStatus.COMPLETED, PaymentStatus.CREATED));

        assertEquals(
                "Invalid status transition for transaction [TXN-003]: COMPLETED → CREATED is not allowed. " +
                        "Allowed transitions from COMPLETED: none (terminal state)",
                ex.getMessage());
    }

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(PaymentStatus.CREATED, PaymentStatus.VALIDATED),
                Arguments.of(PaymentStatus.CREATED, PaymentStatus.FAILED),
                Arguments.of(PaymentStatus.VALIDATED, PaymentStatus.SENT),
                Arguments.of(PaymentStatus.VALIDATED, PaymentStatus.FAILED),
                Arguments.of(PaymentStatus.SENT, PaymentStatus.COMPLETED),
                Arguments.of(PaymentStatus.SENT, PaymentStatus.FAILED),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.CREATED)
        );
    }

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(PaymentStatus.CREATED, PaymentStatus.SENT),
                Arguments.of(PaymentStatus.CREATED, PaymentStatus.COMPLETED),
                Arguments.of(PaymentStatus.VALIDATED, PaymentStatus.CREATED),
                Arguments.of(PaymentStatus.VALIDATED, PaymentStatus.COMPLETED),
                Arguments.of(PaymentStatus.SENT, PaymentStatus.CREATED),
                Arguments.of(PaymentStatus.SENT, PaymentStatus.VALIDATED),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.SENT),
                Arguments.of(PaymentStatus.FAILED, PaymentStatus.COMPLETED),
                Arguments.of(PaymentStatus.COMPLETED, PaymentStatus.CREATED)
        );
    }

    private static String expectedAllowedTransitions(PaymentStatus current) {
        return switch (current) {
            case CREATED -> "[VALIDATED, FAILED]";
            case VALIDATED -> "[SENT, FAILED]";
            case SENT -> "[COMPLETED, FAILED]";
            case FAILED -> "[CREATED]";
            case COMPLETED -> "none (terminal state)";
        };
    }
}

