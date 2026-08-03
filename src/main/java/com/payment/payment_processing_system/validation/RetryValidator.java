package com.payment.payment_processing_system.validation;

import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.InvalidPaymentException;
import com.payment.payment_processing_system.exception.RetryLimitExceededException;
import com.payment.payment_processing_system.model.PaymentTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator for payment retry requests.
 * Enforces all retry business rules before a retry attempt is processed.
 * Does not access the database directly — operates on already-fetched PaymentTransaction objects.
 */
@Slf4j
@Component
public class RetryValidator {

    public static final int MAX_RETRY_COUNT = 3;

    /**
     * Validates that the given transaction is eligible for a retry attempt.
     * Checks are executed in order — the first violation throws immediately.
     *
     * @param transaction the PaymentTransaction to validate for retry
     * @throws InvalidPaymentException    if the transaction is not in FAILED status
     * @throws RetryLimitExceededException if the maximum retry count has been reached
     */
    public void validateRetry(PaymentTransaction transaction) {
        log.debug("Validating retry eligibility for transaction [{}]: status={}, retryCount={}",
                transaction.getTransactionId(),
                transaction.getPaymentStatus(),
                transaction.getRetryCount());

        validateNotCompleted(transaction);
        validateFailedStatus(transaction);
        validateRetryLimitNotExceeded(transaction);

        log.debug("Retry validation passed for transaction [{}]. Attempt #{}.",
                transaction.getTransactionId(), transaction.getRetryCount() + 1);
    }

    // ─── Private Checks ───────────────────────────────────────────────────────

    /**
     * Rule 1: COMPLETED transactions can never be retried.
     */
    private void validateNotCompleted(PaymentTransaction transaction) {
        if (PaymentStatus.COMPLETED.equals(transaction.getPaymentStatus())) {
            throw new InvalidPaymentException(
                    "Transaction [" + transaction.getTransactionId() + "] is already COMPLETED "
                    + "and cannot be retried.");
        }
    }

    /**
     * Rule 2: Only FAILED transactions are eligible for retry.
     */
    private void validateFailedStatus(PaymentTransaction transaction) {
        if (!PaymentStatus.FAILED.equals(transaction.getPaymentStatus())) {
            throw new InvalidPaymentException(
                    "Transaction [" + transaction.getTransactionId() + "] cannot be retried. "
                    + "Only FAILED transactions are eligible. "
                    + "Current status: " + transaction.getPaymentStatus());
        }
    }

    /**
     * Rule 3: Retry count must be strictly less than the maximum allowed retries.
     */
    private void validateRetryLimitNotExceeded(PaymentTransaction transaction) {
        if (transaction.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new RetryLimitExceededException(
                    "Transaction [" + transaction.getTransactionId() + "] has reached the "
                    + "maximum retry limit of " + MAX_RETRY_COUNT + ". "
                    + "Current retry count: " + transaction.getRetryCount());
        }
    }
}

