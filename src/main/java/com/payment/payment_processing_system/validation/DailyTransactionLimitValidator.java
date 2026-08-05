package com.payment.payment_processing_system.validation;

import com.payment.payment_processing_system.exception.DailyTransactionLimitExceededException;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.PaymentTransaction;
import com.payment.payment_processing_system.repository.PaymentTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Validator for daily transaction limits.
 * Ensures that the total transactions from a sender account on a given day
 * does not exceed the configured daily transaction limit for that account.
 */
@Slf4j
@Component
public class DailyTransactionLimitValidator {

    private final PaymentTransactionRepository paymentTransactionRepository;

    public DailyTransactionLimitValidator(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * Validates that the given payment amount does not cause the daily transaction limit to be exceeded.
     * Checks only COMPLETED transactions from the current day (00:00:00 to 23:59:59).
     *
     * @param senderAccount the sender's Account whose limit will be checked
     * @param amount        the payment amount requested
     * @throws DailyTransactionLimitExceededException if adding this amount would exceed the daily limit
     */
    public void validateDailyLimit(Account senderAccount, BigDecimal amount) {
        log.debug("Validating daily transaction limit for account [{}]: requested amount={}, limit={}",
                senderAccount.getAccountNumber(), amount, senderAccount.getDailyTransactionLimit());

        if (senderAccount.getDailyTransactionLimit() == null) {
            throw new DailyTransactionLimitExceededException(
                    "Daily transaction limit is not configured for account: " + senderAccount.getAccountNumber());
        }

        // Get today's date range (00:00:00 to 23:59:59)
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        log.debug("Checking transactions for account [{}] from {} to {}",
                senderAccount.getAccountNumber(), startOfDay, endOfDay);

        // Fetch all COMPLETED transactions from the sender account for today
        List<PaymentTransaction> todayTransactions = paymentTransactionRepository
                .findCompletedTransactionsBySenderAccountOnDate(senderAccount.getId(), startOfDay, endOfDay);

        // Calculate total amount sent today
        BigDecimal totalSentToday = todayTransactions.stream()
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Check if adding the current transaction would exceed the limit
        BigDecimal totalWithCurrentTransaction = totalSentToday.add(amount);

        log.debug("Account [{}]: total sent today={}, requested amount={}, total would be={}, limit={}",
                senderAccount.getAccountNumber(), totalSentToday, amount, totalWithCurrentTransaction,
                senderAccount.getDailyTransactionLimit());

        if (totalWithCurrentTransaction.compareTo(senderAccount.getDailyTransactionLimit()) > 0) {
            String currencyLabel = senderAccount.getCurrency() != null
                    ? senderAccount.getCurrency().name()
                    : "UNKNOWN";
            throw new DailyTransactionLimitExceededException(
                    "Daily transaction limit exceeded for account [" + senderAccount.getAccountNumber() + "]. "
                    + "Total sent today: " + totalSentToday.toPlainString()
                    + ", Requested amount: " + amount.toPlainString()
                    + ", Daily limit: " + senderAccount.getDailyTransactionLimit().toPlainString()
                    + " (" + currencyLabel + ")");
        }

        log.debug("Daily transaction limit validation passed for account [{}].", senderAccount.getAccountNumber());
    }
}

