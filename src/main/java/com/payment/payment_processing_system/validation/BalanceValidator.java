package com.payment.payment_processing_system.validation;

import com.payment.payment_processing_system.exception.InsufficientBalanceException;
import com.payment.payment_processing_system.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Validator for account balance checks.
 * Ensures the sender has sufficient funds before a payment transaction is processed.
 * Does not access the database directly — operates on already-fetched Account objects.
 */
@Slf4j
@Component
public class BalanceValidator {

    /**
     * Validates that the given account has a non-null balance that is
     * greater than or equal to the requested payment amount.
     *
     * @param account the sender Account whose balance will be checked
     * @param amount  the payment amount requested
     * @throws InsufficientBalanceException if the balance is null or less than the amount
     */
    public void validateBalance(Account account, BigDecimal amount) {
        log.debug("Validating balance for account [{}]: required={}, available={}",
                account.getAccountNumber(), amount, account.getBalance());

        validateBalanceNotNull(account);
        validateSufficientFunds(account, amount);

        log.debug("Balance validation passed for account [{}].", account.getAccountNumber());
    }

    // ─── Private Checks ───────────────────────────────────────────────────────

    /**
     * Check 1: Account balance must not be null.
     */
    private void validateBalanceNotNull(Account account) {
        if (account.getBalance() == null) {
            throw new InsufficientBalanceException(
                    "Account balance is null for account: " + account.getAccountNumber());
        }
    }

    /**
     * Check 2: Account balance must be greater than or equal to the payment amount.
     */
    private void validateSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account [" + account.getAccountNumber() + "]. "
                    + "Available: " + account.getBalance().toPlainString()
                    + ", Required: " + amount.toPlainString());
        }
    }
}

