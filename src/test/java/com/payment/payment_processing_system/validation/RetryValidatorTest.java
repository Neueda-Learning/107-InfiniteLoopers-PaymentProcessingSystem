package com.payment.payment_processing_system.validation;

import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.InvalidPaymentException;
import com.payment.payment_processing_system.exception.RetryLimitExceededException;
import com.payment.payment_processing_system.model.PaymentTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RetryValidatorTest {

    private final RetryValidator retryValidator = new RetryValidator();

    @Test
    @DisplayName("FAILED transaction below retry limit should pass validation")
    void validateRetry_whenFailedAndBelowLimit_shouldPass() {
        PaymentTransaction transaction = transaction(PaymentStatus.FAILED, 2, "TXN-001");

        assertDoesNotThrow(() -> retryValidator.validateRetry(transaction));
    }

    @Test
    @DisplayName("COMPLETED transaction should never be retried")
    void validateRetry_whenCompleted_shouldThrowInvalidPaymentException() {
        PaymentTransaction transaction = transaction(PaymentStatus.COMPLETED, 0, "TXN-002");

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () -> retryValidator.validateRetry(transaction));
        assertEquals("Transaction [TXN-002] is already COMPLETED and cannot be retried.", ex.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"CREATED", "VALIDATED", "SENT"})
    @DisplayName("Only FAILED transactions are eligible for retry")
    void validateRetry_whenStatusIsNotFailed_shouldThrowInvalidPaymentException(PaymentStatus status) {
        PaymentTransaction transaction = transaction(status, 0, "TXN-003");

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () -> retryValidator.validateRetry(transaction));
        assertEquals("Transaction [TXN-003] cannot be retried. Only FAILED transactions are eligible. Current status: " + status, ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 10})
    @DisplayName("Retry count at or above maximum should be rejected")
    void validateRetry_whenRetryCountReachedOrExceededLimit_shouldThrowRetryLimitExceededException(int retryCount) {
        PaymentTransaction transaction = transaction(PaymentStatus.FAILED, retryCount, "TXN-004");

        RetryLimitExceededException ex = assertThrows(RetryLimitExceededException.class, () -> retryValidator.validateRetry(transaction));
        assertEquals("Transaction [TXN-004] has reached the maximum retry limit of " + RetryValidator.MAX_RETRY_COUNT + ". Current retry count: " + retryCount, ex.getMessage());
    }

    private PaymentTransaction transaction(PaymentStatus status, int retryCount, String transactionId) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setPaymentStatus(status);
        transaction.setRetryCount(retryCount);
        return transaction;
    }
}

