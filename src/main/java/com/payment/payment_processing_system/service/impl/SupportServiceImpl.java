package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.dto.SupportDashboardResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.mapper.TransactionMapper;
import com.payment.payment_processing_system.model.PaymentTransaction;
import com.payment.payment_processing_system.repository.AccountRepository;
import com.payment.payment_processing_system.repository.CustomerRepository;
import com.payment.payment_processing_system.repository.PaymentTransactionRepository;
import com.payment.payment_processing_system.service.SupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of SupportService.
 * Provides support team operations including dashboard analytics and transaction management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportServiceImpl implements SupportService {

    private final CustomerRepository customerRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    /**
     * Builds the support dashboard with system-wide statistics.
     * Calculates total customers, transactions, successful/failed counts,
     * and total credit/debit amounts from COMPLETED transactions.
     */
    @Override
    public SupportDashboardResponse getDashboard() {
        log.info("Building support dashboard statistics");

        // Total customers registered in the system
        long totalCustomers = customerRepository.count();

        // Total payment transactions (all statuses)
        long totalTransactions = paymentTransactionRepository.count();

        // Transactions that reached COMPLETED status
        long successfulTransactions = paymentTransactionRepository.countByPaymentStatus(PaymentStatus.COMPLETED);

        // Transactions that ended in FAILED status
        long failedTransactions = paymentTransactionRepository.countByPaymentStatus(PaymentStatus.FAILED);

        // Total money received across all accounts (sum of completed transaction amounts = total credits)
        BigDecimal totalCreditAmount = paymentTransactionRepository.sumAmountByStatus(PaymentStatus.COMPLETED);

        // Total money sent across all accounts (same pool; debit == credit at system level for completed)
        BigDecimal totalDebitAmount = paymentTransactionRepository.sumAmountByStatus(PaymentStatus.COMPLETED);

        log.info("Dashboard: customers={}, transactions={}, successful={}, failed={}",
                totalCustomers, totalTransactions, successfulTransactions, failedTransactions);

        return SupportDashboardResponse.builder()
                .totalCustomers(totalCustomers)
                .totalTransactions(totalTransactions)
                .successfulTransactions(successfulTransactions)
                .failedTransactions(failedTransactions)
                .totalCreditAmount(totalCreditAmount != null ? totalCreditAmount : BigDecimal.ZERO)
                .totalDebitAmount(totalDebitAmount != null ? totalDebitAmount : BigDecimal.ZERO)
                .build();
    }

    /**
     * Returns all transactions in the system, sorted newest first.
     */
    @Override
    public List<TransactionResponse> getAllTransactions() {
        log.info("Fetching all transactions for support dashboard");

        return paymentTransactionRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(PaymentTransaction::getCreatedTime).reversed())
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all transactions (sent and received) for a given account number, sorted newest first.
     *
     * @param accountNumber the account number to look up
     * @throws AccountNotFoundException if the account does not exist
     */
    @Override
    public List<TransactionResponse> getTransactionsByCustomer(String accountNumber) {
        log.info("Fetching transactions for account: {}", accountNumber);

        // Validate the account exists before proceeding
        accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with number: " + accountNumber));

        List<PaymentTransaction> sent = paymentTransactionRepository
                .findBySenderAccountAccountNumber(accountNumber);

        List<PaymentTransaction> received = paymentTransactionRepository
                .findByReceiverAccountAccountNumber(accountNumber);

        // Merge and sort by createdTime descending (newest first)
        List<PaymentTransaction> combined = new ArrayList<>(sent);
        combined.addAll(received);

        return combined.stream()
                .sorted(Comparator.comparing(PaymentTransaction::getCreatedTime).reversed())
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all transactions matching the given PaymentStatus, sorted newest first.
     *
     * @param paymentStatus the status to filter by
     */
    @Override
    public List<TransactionResponse> getTransactionsByStatus(PaymentStatus paymentStatus) {
        log.info("Fetching transactions with status: {}", paymentStatus);

        return paymentTransactionRepository.findByPaymentStatus(paymentStatus)
                .stream()
                .sorted(Comparator.comparing(PaymentTransaction::getCreatedTime).reversed())
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns all FAILED transactions sorted by failedTime descending.
     * Each entry includes the failure reason stored at the time of failure.
     */
    @Override
    public List<TransactionResponse> getAuditTrail() {
        log.info("Fetching failed transaction audit trail");

        return paymentTransactionRepository.findByPaymentStatus(PaymentStatus.FAILED)
                .stream()
                .sorted(Comparator.comparing(
                        tx -> tx.getFailedTime() != null ? tx.getFailedTime() : tx.getCreatedTime(),
                        Comparator.reverseOrder()))
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }
}

