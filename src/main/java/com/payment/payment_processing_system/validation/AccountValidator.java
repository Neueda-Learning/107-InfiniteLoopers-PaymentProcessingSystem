package com.payment.payment_processing_system.validation;

import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.exception.InvalidPaymentException;
import com.payment.payment_processing_system.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator for Account entities.
 * Ensures sender and receiver accounts meet all structural and business
 * requirements before a payment transaction is processed.
 * Does not access the database directly — operates on already-fetched Account objects.
 */
@Slf4j
@Component
public class AccountValidator {

    /**
     * Validates that the sender account is present, active, and structurally complete.
     *
     * @param account the sender Account object (may be null if not found)
     * @throws AccountNotFoundException if the account is null
     * @throws InvalidPaymentException  if the account is inactive or has missing required fields
     */
    public void validateSenderAccount(Account account) {
        log.debug("Validating sender account");
        validateAccountNotNull(account, "Sender account does not exist.");
        validateAccountActive(account, "Sender account is inactive and cannot initiate payments.");
        validateAccountNumber(account, "Sender account number must not be empty.");
        validateIfscCode(account, "Sender account IFSC code must not be empty.");
        log.debug("Sender account [{}] passed validation.", account.getAccountNumber());
    }

    /**
     * Validates that the receiver account is present, active, and structurally complete.
     *
     * @param account the receiver Account object (may be null if not found)
     * @throws AccountNotFoundException if the account is null
     * @throws InvalidPaymentException  if the account is inactive or has missing required fields
     */
    public void validateReceiverAccount(Account account) {
        log.debug("Validating receiver account");
        validateAccountNotNull(account, "Receiver account does not exist.");
        validateAccountActive(account, "Receiver account is inactive and cannot receive payments.");
        validateAccountNumber(account, "Receiver account number must not be empty.");
        validateIfscCode(account, "Receiver account IFSC code must not be empty.");
        log.debug("Receiver account [{}] passed validation.", account.getAccountNumber());
    }

    // ─── Private Checks ───────────────────────────────────────────────────────

    /**
     * Check 1: Account object must not be null (i.e. it was found in the database).
     */
    private void validateAccountNotNull(Account account, String message) {
        if (account == null) {
            throw new AccountNotFoundException(message);
        }
    }

    /**
     * Check 2: Account must be marked as active.
     */
    private void validateAccountActive(Account account, String message) {
        if (!account.isActive()) {
            throw new InvalidPaymentException(message);
        }
    }

    /**
     * Check 3: Account number field must not be null or blank.
     */
    private void validateAccountNumber(Account account, String message) {
        if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
            throw new InvalidPaymentException(message);
        }
    }

    /**
     * Check 4: IFSC code must not be null or blank.
     */
    private void validateIfscCode(Account account, String message) {
        if (account.getIfscCode() == null || account.getIfscCode().isBlank()) {
            throw new InvalidPaymentException(message);
        }
    }
}

