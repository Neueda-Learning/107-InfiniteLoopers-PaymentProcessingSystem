package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.dto.PaymentResponse;
import com.payment.payment_processing_system.dto.PreviewPaymentRequest;
import com.payment.payment_processing_system.dto.PreviewPaymentResponse;
import com.payment.payment_processing_system.dto.TransactionStatusHistoryResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.email.EmailService;
import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.exception.DailyTransactionLimitExceededException;
import com.payment.payment_processing_system.exception.DuplicatePaymentException;
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
import com.payment.payment_processing_system.service.CurrencyConversionService;
import com.payment.payment_processing_system.service.PaymentService;
import com.payment.payment_processing_system.validation.AccountValidator;
import com.payment.payment_processing_system.validation.BalanceValidator;
import com.payment.payment_processing_system.validation.PaymentValidator;
import com.payment.payment_processing_system.validation.RetryValidator;
import com.payment.payment_processing_system.validation.StatusTransitionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;
import java.util.Comparator;
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
    private static final int MAX_IDEMPOTENCY_KEY_GENERATION_ATTEMPTS = 5;
    private static final String IDEMPOTENCY_KEY_PATTERN = "[A-Za-z0-9_-]{8,100}";

    private final AccountRepository accountRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TransactionStatusHistoryRepository transactionStatusHistoryRepository;
    private final EmailService emailService;
    private final TransactionMapper transactionMapper;
    private final PaymentValidator paymentValidator;
    private final AccountValidator accountValidator;
    private final BalanceValidator balanceValidator;
    private final CurrencyConversionService currencyConversionService;
    private final RetryValidator retryValidator;
    private final StatusTransitionValidator statusTransitionValidator;

    /**
     * Processes a payment transaction through its full lifecycle:
     * CREATED → VALIDATED → SENT → COMPLETED (or FAILED at any point)
     */
    @Override
    @Transactional
    public PaymentResponse sendMoney(PaymentRequest request) {
        return sendMoney(request, null);
    }

    /**
     * Processes a payment transaction, optionally using a client-supplied
     * idempotency key for safe retries.
     */
    @Override
    @Transactional
    public PaymentResponse sendMoney(PaymentRequest request, String idempotencyKey) {
        PaymentTransaction transaction = null;

        try {
            // Step 0: Validate request fields (no DB access)
            paymentValidator.validate(request);

            // Step 0.1: Resolve idempotency behavior before touching balances.
            String resolvedIdempotencyKey = resolveIdempotencyKey(idempotencyKey);
            PaymentResponse replayResponse = tryHandleIdempotentReplay(request, resolvedIdempotencyKey);
            if (replayResponse != null) {
                return replayResponse;
            }

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

            validateAccountCurrency(senderAccount, "sender");
            validateAccountCurrency(receiverAccount, "receiver");

            // Step 3: Determine conversion and transfer charge (read-only calculation)
            boolean conversionRequired = senderAccount.getCurrency() != receiverAccount.getCurrency();
            BigDecimal exchangeRate = BigDecimal.ONE;
            BigDecimal convertedAmount = request.getAmount();
            BigDecimal transferCharge = BigDecimal.ZERO;

            if (conversionRequired) {
                exchangeRate = currencyConversionService.getExchangeRate(
                        senderAccount.getCurrency(), receiverAccount.getCurrency());
                convertedAmount = currencyConversionService.convertAmount(
                        request.getAmount(), senderAccount.getCurrency(), receiverAccount.getCurrency());
                transferCharge = currencyConversionService.calculateTransferCharge(
                        request.getAmount(), senderAccount.getCurrency(), receiverAccount.getCurrency());
            }

            BigDecimal totalDeducted = request.getAmount().add(transferCharge);

            // Step 5: Validate sender balance (requires DB data)
            balanceValidator.validateBalance(senderAccount, totalDeducted);

            // Step 6: Validate UPI PIN against stored value (requires DB data)
            if (!senderAccount.getUpiPin().equals(request.getUpiPin())) {
                throw new InvalidUpiPinException("Invalid UPI PIN. Authentication failed.");
            }

            // Step 7 & 8: Generate unique transactionId and persist the resolved idempotency key
            String transactionId = generateTransactionId();

            // Step 9: Create PaymentTransaction with status CREATED
            transaction = PaymentTransaction.builder()
                    .transactionId(transactionId)
                    .senderAccount(senderAccount)
                    .receiverAccount(receiverAccount)
                    .amount(request.getAmount())
                    .senderCurrency(senderAccount.getCurrency())
                    .receiverCurrency(receiverAccount.getCurrency())
                    .exchangeRate(exchangeRate)
                    .transferCharge(transferCharge)
                    .convertedAmount(convertedAmount)
                    .description(request.getDescription())
                    .paymentStatus(PaymentStatus.CREATED)
                    .idempotencyKey(resolvedIdempotencyKey)
                    .retryCount(0)
                    .createdTime(LocalDateTime.now())
                    .build();

            try {
                transaction = paymentTransactionRepository.save(transaction);
            } catch (DataIntegrityViolationException ex) {
                PaymentResponse raceReplay = handleIdempotencyRace(request, resolvedIdempotencyKey, ex);
                if (raceReplay != null) {
                    return raceReplay;
                }
                throw new DuplicatePaymentException(
                        "Duplicate payment detected due to idempotency key conflict.", ex);
            }
            recordStatusHistory(transaction, PaymentStatus.CREATED);
            log.info("Transaction created: {}", transactionId);

            // Step 10: Validate payment → status VALIDATED
            statusTransitionValidator.validate(transactionId, transaction.getPaymentStatus(), PaymentStatus.VALIDATED);
            transaction.setPaymentStatus(PaymentStatus.VALIDATED);
            transaction.setValidatedTime(LocalDateTime.now());
            transaction = paymentTransactionRepository.save(transaction);
            recordStatusHistory(transaction, PaymentStatus.VALIDATED);
            log.info("Transaction validated: {}", transactionId);

            // Step 11: Deduct sender balance and credit receiver balance
            senderAccount.setBalance(senderAccount.getBalance().subtract(totalDeducted));
            receiverAccount.setBalance(receiverAccount.getBalance().add(convertedAmount));
            accountRepository.save(senderAccount);
            accountRepository.save(receiverAccount);

            // Step 12: Status → SENT
            statusTransitionValidator.validate(transactionId, transaction.getPaymentStatus(), PaymentStatus.SENT);
            transaction.setPaymentStatus(PaymentStatus.SENT);
            transaction.setSentTime(LocalDateTime.now());
            transaction = paymentTransactionRepository.save(transaction);
            recordStatusHistory(transaction, PaymentStatus.SENT);
            log.info("Transaction sent: {}", transactionId);

            // Step 13: Status → COMPLETED
            statusTransitionValidator.validate(transactionId, transaction.getPaymentStatus(), PaymentStatus.COMPLETED);
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

            return buildPaymentResponse(transaction, "Payment completed successfully.", false);

        // ── BUG FIX: catch ALL validator exceptions explicitly for proper logging ──
        } catch (DuplicatePaymentException ex) {
            log.error("Duplicate payment blocked: {}", ex.getMessage());
            throw ex;

        } catch (AccountNotFoundException
                | InvalidPaymentException
                | InvalidUpiPinException
                | InsufficientBalanceException
                | DailyTransactionLimitExceededException
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

    @Override
    @Transactional(readOnly = true)
    public PreviewPaymentResponse previewPayment(PreviewPaymentRequest request) {
        validatePreviewRequest(request);

        Account senderAccount = getValidatedSenderAccount(request.getSenderAccountNumber());
        Account receiverAccount = getValidatedReceiverAccount(request.getReceiverAccountNumber());

        BigDecimal exchangeRate = currencyConversionService
                .getExchangeRate(senderAccount.getCurrency(), receiverAccount.getCurrency());
        BigDecimal convertedAmount = currencyConversionService
                .convertAmount(request.getAmount(), senderAccount.getCurrency(), receiverAccount.getCurrency());
        BigDecimal transferCharge = currencyConversionService
                .calculateTransferCharge(request.getAmount(), senderAccount.getCurrency(), receiverAccount.getCurrency());

        return PreviewPaymentResponse.builder()
                .senderCurrency(senderAccount.getCurrency())
                .receiverCurrency(receiverAccount.getCurrency())
                .exchangeRate(exchangeRate)
                .originalAmount(request.getAmount())
                .convertedAmount(convertedAmount)
                .transferCharge(transferCharge)
                .totalDeducted(request.getAmount().add(transferCharge))
                .conversionRequired(senderAccount.getCurrency() != receiverAccount.getCurrency())
                .build();
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
     * Retrieve all payment transactions filtered by status.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByStatus(PaymentStatus paymentStatus) {
        return paymentTransactionRepository.findByPaymentStatus(paymentStatus)
                .stream()
                .sorted(Comparator.comparing(PaymentTransaction::getCreatedTime).reversed())
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve status history for a transaction in chronological order.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TransactionStatusHistoryResponse> getTransactionHistory(String transactionId) {
        paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with ID: " + transactionId));

        return transactionStatusHistoryRepository.findByTransactionTransactionId(transactionId)
                .stream()
                .sorted(Comparator.comparing(TransactionStatusHistory::getTimestamp))
                .map(history -> TransactionStatusHistoryResponse.builder()
                        .transactionId(history.getTransaction().getTransactionId())
                        .status(history.getStatus().name())
                        .timestamp(history.getTimestamp())
                        .build())
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

    /**
     * Generate a unique and format-valid idempotency key.
     */
    private String generateUniqueIdempotencyKey() {
        for (int attempt = 1; attempt <= MAX_IDEMPOTENCY_KEY_GENERATION_ATTEMPTS; attempt++) {
            String generatedKey = generateIdempotencyKey();

            if (!generatedKey.matches(IDEMPOTENCY_KEY_PATTERN)) {
                log.warn("Generated idempotency key failed format validation on attempt {}", attempt);
                continue;
            }

            if (!paymentTransactionRepository.existsByIdempotencyKey(generatedKey)) {
                return generatedKey;
            }

            log.warn("Generated idempotency key collision detected on attempt {}", attempt);
        }

        throw new DuplicatePaymentException(
                "Unable to generate a unique idempotency key. Please retry the payment.");
    }

    /**
     * Normalize and validate an incoming idempotency key, or generate one for legacy clients.
     */
    private String resolveIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return generateUniqueIdempotencyKey();
        }

        String normalizedKey = idempotencyKey.trim();
        if (!normalizedKey.matches(IDEMPOTENCY_KEY_PATTERN)) {
            throw new InvalidPaymentException(
                    "Idempotency key must be 8-100 characters and contain only letters, digits, underscores, or hyphens.");
        }

        return normalizedKey;
    }

    /**
     * Return the original payment when the same idempotency key is replayed with the same payload.
     */
    private PaymentResponse tryHandleIdempotentReplay(PaymentRequest request, String idempotencyKey) {
        return paymentTransactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(existingTransaction -> {
                    validateIdempotentRequestMatches(request, existingTransaction);
                    log.info("Returning existing transaction {} for idempotency key {}",
                            existingTransaction.getTransactionId(), idempotencyKey);
                    return buildPaymentResponse(
                            existingTransaction,
                            "Duplicate payment request detected. Returning existing transaction.",
                            true);
                })
                .orElse(null);
    }

    /**
     * Handle DB-level unique races by replaying the existing transaction when safe.
     */
    private PaymentResponse handleIdempotencyRace(
            PaymentRequest request,
            String idempotencyKey,
            DataIntegrityViolationException ex) {

        return paymentTransactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(existingTransaction -> {
                    validateIdempotentRequestMatches(request, existingTransaction);
                    log.info("Resolved concurrent idempotent request using existing transaction {}",
                            existingTransaction.getTransactionId());
                    return buildPaymentResponse(
                            existingTransaction,
                            "Duplicate payment request detected. Returning existing transaction.",
                            true);
                })
                .orElseThrow(() -> new DuplicatePaymentException(
                        "Duplicate payment detected due to idempotency key conflict.", ex));
    }

    /**
     * Ensure a reused idempotency key is not paired with a different payment payload.
     */
    private void validateIdempotentRequestMatches(PaymentRequest request, PaymentTransaction existingTransaction) {
        boolean sameRequest = Objects.equals(request.getSenderAccountNumber(), existingTransaction.getSenderAccount().getAccountNumber())
                && Objects.equals(request.getReceiverAccountNumber(), existingTransaction.getReceiverAccount().getAccountNumber())
                && Objects.equals(request.getReceiverIfscCode(), existingTransaction.getReceiverAccount().getIfscCode())
                && request.getAmount() != null
                && request.getAmount().compareTo(existingTransaction.getAmount()) == 0
                && Objects.equals(normalizeDescription(request.getDescription()), normalizeDescription(existingTransaction.getDescription()));

        if (!sameRequest) {
            throw new DuplicatePaymentException(
                    "The provided idempotency key has already been used for a different payment request.");
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private void validateAccountCurrency(Account account, String role) {
        if (account.getCurrency() == null) {
            throw new InvalidPaymentException("Currency is not configured for " + role + " account: "
                    + account.getAccountNumber());
        }
    }

    private void validatePreviewRequest(PreviewPaymentRequest request) {
        if (request == null) {
            throw new InvalidPaymentException("Preview payment request must not be null.");
        }
        if (request.getSenderAccountNumber() == null || request.getSenderAccountNumber().isBlank()) {
            throw new InvalidPaymentException("Sender account number must not be empty.");
        }
        if (request.getReceiverAccountNumber() == null || request.getReceiverAccountNumber().isBlank()) {
            throw new InvalidPaymentException("Receiver account number must not be empty.");
        }
        if (request.getSenderAccountNumber().equalsIgnoreCase(request.getReceiverAccountNumber())) {
            throw new InvalidPaymentException("Sender and receiver account numbers cannot be the same.");
        }
        if (request.getAmount() == null) {
            throw new InvalidPaymentException("Transaction amount must not be null.");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException(
                    "Transaction amount must be greater than zero. Provided: " + request.getAmount().toPlainString());
        }
    }

    private Account getValidatedSenderAccount(String senderAccountNumber) {
        Account senderAccount = accountRepository.findByAccountNumber(senderAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found: " + senderAccountNumber));
        accountValidator.validateSenderAccount(senderAccount);
        return senderAccount;
    }

    private Account getValidatedReceiverAccount(String receiverAccountNumber) {
        Account receiverAccount = accountRepository.findByAccountNumber(receiverAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found: " + receiverAccountNumber));
        accountValidator.validateReceiverAccount(receiverAccount);
        return receiverAccount;
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
            statusTransitionValidator.validate(
                    transaction.getTransactionId(), transaction.getPaymentStatus(), PaymentStatus.FAILED);
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
    private PaymentResponse buildPaymentResponse(PaymentTransaction transaction, String message, boolean idempotentReplay) {
        BigDecimal transferCharge = transaction.getTransferCharge() != null ? transaction.getTransferCharge() : BigDecimal.ZERO;
        BigDecimal convertedAmount = transaction.getConvertedAmount() != null ? transaction.getConvertedAmount() : transaction.getAmount();
        boolean conversionRequired = transaction.getSenderCurrency() != null
                && transaction.getReceiverCurrency() != null
                && transaction.getSenderCurrency() != transaction.getReceiverCurrency();

        return PaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .paymentStatus(transaction.getPaymentStatus().name())
                .message(message)
                .idempotentReplay(idempotentReplay)
                .amount(transaction.getAmount())
                .senderAccountNumber(transaction.getSenderAccount().getAccountNumber())
                .receiverAccountNumber(transaction.getReceiverAccount().getAccountNumber())
                .senderCurrency(transaction.getSenderCurrency())
                .receiverCurrency(transaction.getReceiverCurrency())
                .exchangeRate(transaction.getExchangeRate())
                .transferCharge(transferCharge)
                .convertedAmount(convertedAmount)
                .totalDeducted(transaction.getAmount().add(transferCharge))
                .conversionRequired(conversionRequired)
                .transactionTime(transaction.getCompletedTime() != null
                        ? transaction.getCompletedTime()
                        : transaction.getCreatedTime())
                .build();
    }
}
