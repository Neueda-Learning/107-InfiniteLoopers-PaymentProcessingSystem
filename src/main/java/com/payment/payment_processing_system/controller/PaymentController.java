package com.payment.payment_processing_system.controller;

import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.dto.PreviewPaymentRequest;
import com.payment.payment_processing_system.dto.PreviewPaymentResponse;
import com.payment.payment_processing_system.dto.PaymentResponse;
import jakarta.validation.Valid;
import com.payment.payment_processing_system.dto.TransactionStatusHistoryResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for payment transaction operations.
 * Exposes endpoints for initiating, retrying, and retrieving payment transactions.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/send
     * Initiate a new payment transaction.
     *
     * @param paymentRequest the payment details including sender, receiver, amount and UPI PIN
     * @return 201 CREATED with payment execution details
     */
    @PostMapping("/send")
    public ResponseEntity<PaymentResponse> sendMoney(@Valid @RequestBody PaymentRequest paymentRequest) {
        log.info("POST /api/payments/send - Initiating payment from account: {}",
                paymentRequest.getSenderAccountNumber());
        PaymentResponse response = paymentService.sendMoney(paymentRequest);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * POST /api/payments/preview
     * Preview payment conversion and charges without deducting funds or creating transactions.
     */
    @PostMapping("/preview")
    public ResponseEntity<PreviewPaymentResponse> previewPayment(
            @Valid @RequestBody PreviewPaymentRequest previewPaymentRequest) {
        log.info("POST /api/payments/preview - Previewing payment from account: {}",
                previewPaymentRequest.getSenderAccountNumber());
        PreviewPaymentResponse response = paymentService.previewPayment(previewPaymentRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/retry/{transactionId}
     * Retry a previously failed payment transaction.
     *
     * @param transactionId the ID of the failed transaction to retry
     * @return 200 OK with PaymentResponse containing the retry transaction confirmation
     */
    @PostMapping("/retry/{transactionId}")
    public ResponseEntity<PaymentResponse> retryPayment(@PathVariable String transactionId) {
        log.info("POST /api/payments/retry/{} - Retrying failed payment", transactionId);
        PaymentResponse response = paymentService.retryPayment(transactionId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/payments/{transactionId}
     * Retrieve details of a specific payment transaction.
     *
     * @param transactionId the unique transaction ID
     * @return 200 OK with the matching TransactionResponse
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        log.info("GET /api/payments/{} - Fetching transaction details", transactionId);
        TransactionResponse transaction = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * GET /api/payments
     * Retrieve all payment transactions in the system.
     *
     * @return 200 OK with list of all TransactionResponse objects
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        log.info("GET /api/payments - Fetching all payment transactions");
        List<TransactionResponse> transactions = paymentService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/payments/status/{status}
     * Retrieve all payment transactions filtered by status.
     *
     * @param status the payment status to filter by
     * @return 200 OK with list of matching TransactionResponse objects
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByStatus(
            @PathVariable PaymentStatus status) {
        log.info("GET /api/payments/status/{} - Fetching transactions by status", status);
        List<TransactionResponse> transactions = paymentService.getTransactionsByStatus(status);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/payments/{transactionId}/history
     * Retrieve status transition history for a specific transaction.
     *
     * @param transactionId the unique transaction ID
     * @return 200 OK with list of status history entries in chronological order
     */
    @GetMapping("/{transactionId}/history")
    public ResponseEntity<List<TransactionStatusHistoryResponse>> getTransactionHistory(
            @PathVariable String transactionId) {
        log.info("GET /api/payments/{}/history - Fetching transaction status history", transactionId);
        List<TransactionStatusHistoryResponse> history = paymentService.getTransactionHistory(transactionId);
        return ResponseEntity.ok(history);
    }
}

