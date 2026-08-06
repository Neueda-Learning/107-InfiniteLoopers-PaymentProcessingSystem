package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.dto.PaymentRequest;
import com.payment.payment_processing_system.dto.PaymentResponse;
import com.payment.payment_processing_system.dto.PreviewPaymentRequest;
import com.payment.payment_processing_system.dto.PreviewPaymentResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.dto.TransactionStatusHistoryResponse;
import com.payment.payment_processing_system.email.EmailService;
import com.payment.payment_processing_system.enums.CurrencyType;
import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.InvalidPaymentException;
import com.payment.payment_processing_system.exception.TransactionNotFoundException;
import com.payment.payment_processing_system.mapper.TransactionMapper;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.Customer;
import com.payment.payment_processing_system.model.PaymentTransaction;
import com.payment.payment_processing_system.model.TransactionStatusHistory;
import com.payment.payment_processing_system.repository.AccountRepository;
import com.payment.payment_processing_system.repository.PaymentTransactionRepository;
import com.payment.payment_processing_system.repository.TransactionStatusHistoryRepository;
import com.payment.payment_processing_system.service.CurrencyConversionService;
import com.payment.payment_processing_system.validation.AccountValidator;
import com.payment.payment_processing_system.validation.BalanceValidator;
import com.payment.payment_processing_system.validation.PaymentValidator;
import com.payment.payment_processing_system.validation.RetryValidator;
import com.payment.payment_processing_system.validation.StatusTransitionValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private TransactionStatusHistoryRepository transactionStatusHistoryRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private PaymentValidator paymentValidator;

    @Mock
    private AccountValidator accountValidator;

    @Mock
    private BalanceValidator balanceValidator;

    @Mock
    private CurrencyConversionService currencyConversionService;

    @Mock
    private RetryValidator retryValidator;

    @Mock
    private StatusTransitionValidator statusTransitionValidator;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("sendMoney should complete payment lifecycle for valid request")
    void sendMoney_whenValidRequest_shouldCompleteTransaction() {
        PaymentRequest request = validRequest();
        Account sender = senderAccount();
        Account receiver = receiverAccount();

        when(paymentTransactionRepository.findByIdempotencyKey("IDEMPKEY123")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("100000000002")).thenReturn(Optional.of(receiver));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionStatusHistoryRepository.save(any(TransactionStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.sendMoney(request, "IDEMPKEY123");

        assertEquals("COMPLETED", response.getPaymentStatus());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertFalse(response.isIdempotentReplay());
        assertEquals("100000000001", response.getSenderAccountNumber());
        assertEquals("100000000002", response.getReceiverAccountNumber());

        verify(statusTransitionValidator, times(3)).validate(anyString(), any(PaymentStatus.class), any(PaymentStatus.class));
        verify(transactionStatusHistoryRepository, times(4)).save(any(TransactionStatusHistory.class));
    }

    @Test
    @DisplayName("sendMoney should apply currency conversion and charge for cross-currency transfer")
    void sendMoney_whenCrossCurrency_shouldDeductChargeAndCreditConvertedAmount() {
        PaymentRequest request = validRequest();
        Account sender = senderAccount();
        sender.setCurrency(CurrencyType.USD);
        sender.setBalance(new BigDecimal("1000.00"));
        Account receiver = receiverAccount();
        receiver.setCurrency(CurrencyType.INR);
        receiver.setBalance(new BigDecimal("30000.00"));

        when(paymentTransactionRepository.findByIdempotencyKey("IDEMPKEY123")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("100000000002")).thenReturn(Optional.of(receiver));
        when(currencyConversionService.getExchangeRate(CurrencyType.USD, CurrencyType.INR)).thenReturn(new BigDecimal("87"));
        when(currencyConversionService.convertAmount(new BigDecimal("100.00"), CurrencyType.USD, CurrencyType.INR))
                .thenReturn(new BigDecimal("8700.0000"));
        when(currencyConversionService.calculateTransferCharge(new BigDecimal("100.00"), CurrencyType.USD, CurrencyType.INR))
                .thenReturn(new BigDecimal("2.0000"));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionStatusHistoryRepository.save(any(TransactionStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.sendMoney(request, "IDEMPKEY123");

        assertEquals("COMPLETED", response.getPaymentStatus());
        assertEquals(new BigDecimal("898.0000"), sender.getBalance());
        assertEquals(new BigDecimal("38700.0000"), receiver.getBalance());

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository, times(4)).save(transactionCaptor.capture());
        PaymentTransaction created = transactionCaptor.getAllValues().get(0);
        assertEquals(CurrencyType.USD, created.getSenderCurrency());
        assertEquals(CurrencyType.INR, created.getReceiverCurrency());
        assertEquals(new BigDecimal("87"), created.getExchangeRate());
        assertEquals(new BigDecimal("2.0000"), created.getTransferCharge());
        assertEquals(new BigDecimal("8700.0000"), created.getConvertedAmount());
    }

    @Test
    @DisplayName("sendMoney should return replay response for same idempotency key and payload")
    void sendMoney_whenIdempotencyKeyAlreadyExistsWithSamePayload_shouldReturnReplay() {
        PaymentRequest request = validRequest();
        PaymentTransaction existing = completedTransaction("TXN-REPLAY");

        when(paymentTransactionRepository.findByIdempotencyKey("IDEMPKEY123")).thenReturn(Optional.of(existing));

        PaymentResponse response = paymentService.sendMoney(request, "IDEMPKEY123");

        assertTrue(response.isIdempotentReplay());
        assertEquals("TXN-REPLAY", response.getTransactionId());
        assertEquals("Duplicate payment request detected. Returning existing transaction.", response.getMessage());
        verify(accountRepository, never()).findByAccountNumber(anyString());
    }

    @Test
    @DisplayName("previewPayment should return conversion details without creating transaction")
    void previewPayment_whenValidCrossCurrencyRequest_shouldReturnPreviewOnly() {
        PreviewPaymentRequest request = validPreviewRequest();
        Account sender = senderAccount();
        sender.setCurrency(CurrencyType.USD);
        Account receiver = receiverAccount();
        receiver.setCurrency(CurrencyType.INR);

        when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("100000000002")).thenReturn(Optional.of(receiver));
        when(currencyConversionService.getExchangeRate(CurrencyType.USD, CurrencyType.INR))
                .thenReturn(new BigDecimal("87"));
        when(currencyConversionService.convertAmount(new BigDecimal("100.00"), CurrencyType.USD, CurrencyType.INR))
                .thenReturn(new BigDecimal("8700.0000"));
        when(currencyConversionService.calculateTransferCharge(new BigDecimal("100.00"), CurrencyType.USD, CurrencyType.INR))
                .thenReturn(new BigDecimal("2.0000"));

        PreviewPaymentResponse response = paymentService.previewPayment(request);

        assertEquals(CurrencyType.USD, response.getSenderCurrency());
        assertEquals(CurrencyType.INR, response.getReceiverCurrency());
        assertEquals(new BigDecimal("87"), response.getExchangeRate());
        assertEquals(new BigDecimal("100.00"), response.getOriginalAmount());
        assertEquals(new BigDecimal("8700.0000"), response.getConvertedAmount());
        assertEquals(new BigDecimal("2.0000"), response.getTransferCharge());
        assertEquals(new BigDecimal("102.0000"), response.getTotalDeducted());
        assertTrue(response.isConversionRequired());

        verify(accountValidator).validateSenderAccount(sender);
        verify(accountValidator).validateReceiverAccount(receiver);
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(transactionStatusHistoryRepository, never()).save(any(TransactionStatusHistory.class));
        verify(accountRepository, never()).save(any(Account.class));
        verify(emailService, never()).sendNotification(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("previewPayment should return domestic preview without conversion charge")
    void previewPayment_whenSameCurrency_shouldReturnDomesticPreview() {
        PreviewPaymentRequest request = validPreviewRequest();
        Account sender = senderAccount();
        sender.setCurrency(CurrencyType.INR);
        Account receiver = receiverAccount();
        receiver.setCurrency(CurrencyType.INR);

        when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("100000000002")).thenReturn(Optional.of(receiver));
        when(currencyConversionService.getExchangeRate(CurrencyType.INR, CurrencyType.INR))
                .thenReturn(BigDecimal.ONE);
        when(currencyConversionService.convertAmount(new BigDecimal("100.00"), CurrencyType.INR, CurrencyType.INR))
                .thenReturn(new BigDecimal("100.0000"));
        when(currencyConversionService.calculateTransferCharge(new BigDecimal("100.00"), CurrencyType.INR, CurrencyType.INR))
                .thenReturn(new BigDecimal("0.0000"));

        PreviewPaymentResponse response = paymentService.previewPayment(request);

        assertFalse(response.isConversionRequired());
        assertEquals(new BigDecimal("100.0000"), response.getConvertedAmount());
        assertEquals(new BigDecimal("0.0000"), response.getTransferCharge());
        assertEquals(new BigDecimal("100.0000"), response.getTotalDeducted());
    }

    @Test
    @DisplayName("sendMoney should reject malformed idempotency key")
    void sendMoney_whenIdempotencyKeyFormatInvalid_shouldThrowInvalidPaymentException() {
        PaymentRequest request = validRequest();

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class,
                () -> paymentService.sendMoney(request, "invalid key!"));

        assertEquals("Idempotency key must be 8-100 characters and contain only letters, digits, underscores, or hyphens.", ex.getMessage());
    }

    @Test
    @DisplayName("retryPayment should rebuild request and delegate to sendMoney")
    void retryPayment_whenEligible_shouldCallSendMoney() {
        Account sender = senderAccount();
        Account receiver = receiverAccount();

        PaymentTransaction failed = new PaymentTransaction();
        failed.setTransactionId("TXN-FAILED-1");
        failed.setSenderAccount(sender);
        failed.setReceiverAccount(receiver);
        failed.setAmount(new BigDecimal("120.00"));
        failed.setDescription("Retry this payment");
        failed.setPaymentStatus(PaymentStatus.FAILED);
        failed.setRetryCount(1);

        when(paymentTransactionRepository.findByTransactionId("TXN-FAILED-1")).thenReturn(Optional.of(failed));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentServiceImpl spyService = spy(paymentService);
        PaymentResponse mockedResponse = PaymentResponse.builder()
                .transactionId("TXN-NEW")
                .paymentStatus("COMPLETED")
                .message("ok")
                .build();

        doReturn(mockedResponse).when(spyService).sendMoney(any(PaymentRequest.class));

        PaymentResponse response = spyService.retryPayment("TXN-FAILED-1");

        assertEquals("TXN-NEW", response.getTransactionId());
        verify(retryValidator).validateRetry(failed);
        verify(paymentTransactionRepository).save(failed);
        verify(spyService).sendMoney(any(PaymentRequest.class));
    }

    @Test
    @DisplayName("retryPayment should throw when transaction does not exist")
    void retryPayment_whenTransactionMissing_shouldThrowTransactionNotFoundException() {
        when(paymentTransactionRepository.findByTransactionId("TXN-MISSING")).thenReturn(Optional.empty());

        TransactionNotFoundException ex = assertThrows(TransactionNotFoundException.class,
                () -> paymentService.retryPayment("TXN-MISSING"));

        assertEquals("Transaction not found with ID: TXN-MISSING", ex.getMessage());
    }

    @Test
    @DisplayName("getTransactionsByStatus should return newest-first order")
    void getTransactionsByStatus_shouldSortByCreatedTimeDescending() {
        PaymentTransaction oldTx = completedTransaction("TXN-OLD");
        oldTx.setCreatedTime(LocalDateTime.of(2026, 8, 4, 10, 0, 0));

        PaymentTransaction newTx = completedTransaction("TXN-NEW");
        newTx.setCreatedTime(LocalDateTime.of(2026, 8, 4, 11, 0, 0));

        when(paymentTransactionRepository.findByPaymentStatus(PaymentStatus.COMPLETED)).thenReturn(List.of(oldTx, newTx));
        when(transactionMapper.toTransactionResponse(oldTx)).thenReturn(TransactionResponse.builder().transactionId("TXN-OLD").build());
        when(transactionMapper.toTransactionResponse(newTx)).thenReturn(TransactionResponse.builder().transactionId("TXN-NEW").build());

        List<TransactionResponse> responses = paymentService.getTransactionsByStatus(PaymentStatus.COMPLETED);

        assertEquals(2, responses.size());
        assertEquals("TXN-NEW", responses.get(0).getTransactionId());
        assertEquals("TXN-OLD", responses.get(1).getTransactionId());
    }

    @Test
    @DisplayName("getTransactionHistory should return chronological order")
    void getTransactionHistory_shouldSortByTimestampAscending() {
        PaymentTransaction tx = completedTransaction("TXN-HISTORY");

        TransactionStatusHistory later = new TransactionStatusHistory();
        later.setTransaction(tx);
        later.setStatus(PaymentStatus.SENT);
        later.setTimestamp(LocalDateTime.of(2026, 8, 4, 10, 0, 7));

        TransactionStatusHistory earlier = new TransactionStatusHistory();
        earlier.setTransaction(tx);
        earlier.setStatus(PaymentStatus.CREATED);
        earlier.setTimestamp(LocalDateTime.of(2026, 8, 4, 10, 0, 0));

        when(paymentTransactionRepository.findByTransactionId("TXN-HISTORY")).thenReturn(Optional.of(tx));
        when(transactionStatusHistoryRepository.findByTransactionTransactionId("TXN-HISTORY")).thenReturn(List.of(later, earlier));

        List<TransactionStatusHistoryResponse> history = paymentService.getTransactionHistory("TXN-HISTORY");

        assertEquals(2, history.size());
        assertEquals("CREATED", history.get(0).getStatus());
        assertEquals("SENT", history.get(1).getStatus());
    }

    private PaymentRequest validRequest() {
        return PaymentRequest.builder()
                .senderAccountNumber("100000000001")
                .receiverAccountNumber("100000000002")
                .receiverIfscCode("HDFC0005678")
                .amount(new BigDecimal("100.00"))
                .description("Test payment")
                .upiPin("1234")
                .build();
    }

    private PreviewPaymentRequest validPreviewRequest() {
        return PreviewPaymentRequest.builder()
                .senderAccountNumber("100000000001")
                .receiverAccountNumber("100000000002")
                .amount(new BigDecimal("100.00"))
                .build();
    }

    private Account senderAccount() {
        Customer customer = new Customer();
        customer.setCustomerName("Alice Johnson");
        customer.setEmail("alice.johnson@example.com");

        Account sender = new Account();
        sender.setAccountNumber("100000000001");
        sender.setIfscCode("SBIN0001234");
        sender.setBalance(new BigDecimal("50000.00"));
        sender.setCurrency(CurrencyType.INR);
        sender.setUpiPin("1234");
        sender.setCustomer(customer);
        return sender;
    }

    private Account receiverAccount() {
        Customer customer = new Customer();
        customer.setCustomerName("Bob Smith");
        customer.setEmail("bob.smith@example.com");

        Account receiver = new Account();
        receiver.setAccountNumber("100000000002");
        receiver.setIfscCode("HDFC0005678");
        receiver.setBalance(new BigDecimal("30000.00"));
        receiver.setCurrency(CurrencyType.INR);
        receiver.setUpiPin("5678");
        receiver.setCustomer(customer);
        return receiver;
    }

    private PaymentTransaction completedTransaction(String transactionId) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(transactionId);
        tx.setSenderAccount(senderAccount());
        tx.setReceiverAccount(receiverAccount());
        tx.setAmount(new BigDecimal("100.00"));
        tx.setDescription("Test payment");
        tx.setPaymentStatus(PaymentStatus.COMPLETED);
        tx.setCreatedTime(LocalDateTime.of(2026, 8, 4, 10, 0, 0));
        tx.setCompletedTime(LocalDateTime.of(2026, 8, 4, 10, 0, 10));
        return tx;
    }
}


