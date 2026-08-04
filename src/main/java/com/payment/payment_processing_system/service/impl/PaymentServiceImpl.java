package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.dto.PaymentResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.email.EmailService;
import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.exception.InsufficientBalanceException;
import com.payment.payment_processing_system.exception.InvalidPaymentException;
import com.payment.payment_processing_system.exception.InvalidUpiPinException;
import com.payment.payment_processing_system.exception.RetryLimitExceededException;
import com.payment.payment_processing_system.exception.TransactionNotFoundException;
import com.payment.payment_processing_system.mapper.TransactionMapper;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.PaymentTransaction;
import com.payment.payment_processing_system.model.TransactionStatusHistory;
import com.payment.payment_processing_system.repository.AccountRepository;
import com.payment.payment_processing_system.repository.PaymentTransactionRepository;
import com.payment.payment_processing_system.repository.TransactionStatusHistoryRepository;
import com.payment.payment_processing_system.service.PaymentService;
import com.payment.payment_processing_system.validation.AccountValidator;
import com.payment.payment_processing_system.validation.BalanceValidator;
import com.payment.payment_processing_system.validation.PaymentValidator;
import com.payment.payment_processing_system.validation.RetryValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of PaymentService.
 * Handles the complete payment transaction lifecycle with full state machine management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000");

    private final AccountRepository accountRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TransactionStatusHistoryRepository transactionStatusHistoryRepository;
    private final EmailService emailService;
    private final TransactionMapper transactionMapper;
    private final PaymentValidator paymentValidator;
    private final AccountValidator accountValidator;
    private final BalanceValidator balanceValidator;
    private final RetryValidator retryValidator;

    /**
     * Processes a payment transaction through its full lifecycle:
     * CREATED → VALIDATED → SENT → COMPLETED (or FAILED at any point)
     */
    @Override
    @Transactional
    public PaymentResponse sendMoney(PaymentRequest request) {
        PaymentTransaction transaction = null;

        try {
            // Step 0: Validate request fields (no DB access)
            paymentValidator.validate(request);

            // Step 1: Fetch and validate sender account
            Account senderAccount = accountRepository.findByAccountNumber(request.getSenderAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Sender account not found: " + request.getSenderAccountNumber()));
            accountValidator.validateSenderAccount(senderAccount);

            // Step 2: Fetch and validate receiver account
            Account receiverAccount = accountRepository.findByAccountNumber(request.getReceiverAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Receiver account not found: " + request.getReceiverAccountNumber()));
            accountValidator.validateReceiverAccount(receiverAccount);

            // Step 5: Validate sender balance (requires DB data)
            balanceValidator.validateBalance(senderAccount, request.getAmount());

            // Step 6: Validate UPI PIN against stored value (requires DB data)
            if (!senderAccount.getUpiPin().equals(request.getUpiPin())) {
                throw new InvalidUpiPinException("Invalid UPI PIN. Authentication failed.");
            }

            // Step 7 & 8: Generate unique transactionId and idempotencyKey
            String transactionId = generateTransactionId();
            String idempotencyKey = generateIdempotencyKey();

            // Step 9: Create PaymentTransaction with status CREATED
            transaction = PaymentTransaction.builder()
                    .transactionId(transactionId)
                    .senderAccount(senderAccount)
                    .receiverAccount(receiverAccount)
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .paymentStatus(PaymentStatus.CREATED)
                    .idempotencyKey(idempotencyKey)
                    .retryCount(0)
                    .createdTime(LocalDateTime.now())
                    .build();

            transaction = paymentTransactionRepository.save(transaction);
            recordStatusHistory(transaction, PaymentStatus.CREATED);
            log.info("Transaction created: {}", transactionId);

            // Step 10: Validate payment → status VALIDATED
            transaction.setPaymentStatus(PaymentStatus.VALIDATED);
            transaction.setValidatedTime(LocalDateTime.now());
            transaction = paymentTransactionRepository.save(transaction);
            recordStatusHistory(transaction, PaymentStatus.VALIDATED);
            log.info("Transaction validated: {}", transactionId);

            // Step 11: Deduct sender balance and credit receiver balance
            senderAccount.setBalance(senderAccount.getBalance().subtract(request.getAmount()));
            receiverAccount.setBalance(receiverAccount.getBalance().add(request.getAmount()));
            accountRepository.save(senderAccount);
            accountRepository.save(receiverAccount);

            // Step 12: Status → SENT
            transaction.setPaymentStatus(PaymentStatus.SENT);
            transaction.setSentTime(LocalDateTime.now());
            transaction = paymentTransactionRepository.save(transaction);
            recordStatusHistory(transaction, PaymentStatus.SENT);
            log.info("Transaction sent: {}", transactionId);

            // Step 13: Status → COMPLETED
            transaction.setPaymentStatus(PaymentStatus.COMPLETED);
            transaction.setCompletedTime(LocalDateTime.now());
            transaction = paymentTransactionRepository.save(transaction);
            recordStatusHistory(transaction, PaymentStatus.COMPLETED);
            log.info("Transaction completed: {}", transactionId);

            // Step 14: Send email notification for high-value transactions (amount > 10000)
            if (request.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
                try {
                    String senderEmail = senderAccount.getCustomer().getEmail();
                    String senderName = senderAccount.getCustomer().getCustomerName();
                    emailService.sendNotification(senderEmail, senderName, transactionId,
                            request.getAmount().toPlainString());
                    log.info("High-value transaction notification sent for: {}", transactionId);
                } catch (Exception emailEx) {
                    // Email failure must never roll back a completed transaction
                    log.warn("Email notification failed for transaction {}: {}", transactionId, emailEx.getMessage());
                }
            }

            return buildPaymentResponse(transaction, "Payment completed successfully.");

        // ── BUG FIX: catch ALL validator exceptions explicitly for proper logging ──
        } catch (AccountNotFoundException
                | InvalidPaymentException
                | InvalidUpiPinException
                | InsufficientBalanceException
                | RetryLimitExceededException ex) {
            log.error("Payment failed during validation [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage());
            if (transaction != null) {
                markAsFailed(transaction);
            }
            throw ex;

        } catch (Exception ex) {
            log.error("Unexpected error during payment processing: {}", ex.getMessage(), ex);
            if (transaction != null) {
                markAsFailed(transaction);
            }
            throw ex;
        }
    }

    /**
     * Retries a FAILED payment transaction.
     * All retry rules are enforced by RetryValidator.
     */
    @Override
    @Transactional
    public PaymentResponse retryPayment(String transactionId) {
        PaymentTransaction original = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with ID: " + transactionId));

        // BUG FIX: delegate entirely to RetryValidator — removed duplicate inline checks
        retryValidator.validateRetry(original);

        // Increment retry count on the original transaction
        original.setRetryCount(original.getRetryCount() + 1);
        paymentTransactionRepository.save(original);
        log.info("Retrying transaction: {} (attempt #{})", transactionId, original.getRetryCount());

        // Build a new PaymentRequest from the original transaction and retry
        PaymentRequest retryRequest = PaymentRequest.builder()
                .senderAccountNumber(original.getSenderAccount().getAccountNumber())
                .receiverAccountNumber(original.getReceiverAccount().getAccountNumber())
                .receiverIfscCode(original.getReceiverAccount().getIfscCode())
                .amount(original.getAmount())
                .description(original.getDescription())
                .upiPin(original.getSenderAccount().getUpiPin())
                .build();

        return sendMoney(retryRequest);
    }

    /**
     * Retrieve details of a specific transaction.
     */
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with ID: " + transactionId));
        return transactionMapper.toTransactionResponse(transaction);
    }

    /**
     * Retrieve all payment transactions.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions() {
        return paymentTransactionRepository.findAll()
                .stream()
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Generate a unique transaction ID using UUID.
     */
    @Override
    public String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    /**
     * Generate a unique idempotency key using UUID.
     */
    @Override
    public String generateIdempotencyKey() {
        return "IDEM-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    // ─── Private helper methods ────────────────────────────────────────────────

    /**
     * Records a status transition into TransactionStatusHistory.
     */
    private void recordStatusHistory(PaymentTransaction transaction, PaymentStatus status) {
        TransactionStatusHistory history = new TransactionStatusHistory();
        history.setTransaction(transaction);
        history.setStatus(status);
        history.setTimestamp(LocalDateTime.now());
        transactionStatusHistoryRepository.save(history);
    }

    /**
     * Marks a transaction as FAILED and records a history entry.
     */
    private void markAsFailed(PaymentTransaction transaction) {
        try {
            transaction.setPaymentStatus(PaymentStatus.FAILED);
            transaction.setFailedTime(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);
            recordStatusHistory(transaction, PaymentStatus.FAILED);
            log.info("Transaction marked as FAILED: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to update transaction status to FAILED: {}", e.getMessage(), e);
        }
    }

    /**
     * Builds a PaymentResponse from a completed or failed transaction.
     */
    private PaymentResponse buildPaymentResponse(PaymentTransaction transaction, String message) {
        return PaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .paymentStatus(transaction.getPaymentStatus().name())
                .message(message)
                .amount(transaction.getAmount())
                .senderAccountNumber(transaction.getSenderAccount().getAccountNumber())
                .receiverAccountNumber(transaction.getReceiverAccount().getAccountNumber())
                .transactionTime(transaction.getCompletedTime() != null
                        ? transaction.getCompletedTime()
                        : transaction.getCreatedTime())
                .build();
    }
}
