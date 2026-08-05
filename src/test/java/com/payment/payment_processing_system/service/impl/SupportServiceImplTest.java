package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.dto.SupportDashboardResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.mapper.TransactionMapper;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.PaymentTransaction;
import com.payment.payment_processing_system.repository.AccountRepository;
import com.payment.payment_processing_system.repository.CustomerRepository;
import com.payment.payment_processing_system.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private SupportServiceImpl supportService;

    @Test
    @DisplayName("getDashboard should return aggregate metrics")
    void getDashboard_shouldReturnCalculatedMetrics() {
        when(customerRepository.count()).thenReturn(10L);
        when(paymentTransactionRepository.count()).thenReturn(40L);
        when(paymentTransactionRepository.countByPaymentStatus(PaymentStatus.COMPLETED)).thenReturn(35L);
        when(paymentTransactionRepository.countByPaymentStatus(PaymentStatus.FAILED)).thenReturn(5L);
        when(paymentTransactionRepository.sumAmountByStatus(PaymentStatus.COMPLETED)).thenReturn(new BigDecimal("12500.50"));

        SupportDashboardResponse response = supportService.getDashboard();

        assertEquals(10L, response.getTotalCustomers());
        assertEquals(40L, response.getTotalTransactions());
        assertEquals(35L, response.getSuccessfulTransactions());
        assertEquals(5L, response.getFailedTransactions());
        assertEquals(new BigDecimal("12500.50"), response.getTotalCreditAmount());
        assertEquals(new BigDecimal("12500.50"), response.getTotalDebitAmount());
    }

    @Test
    @DisplayName("getDashboard should default null sums to zero")
    void getDashboard_whenSumsAreNull_shouldReturnZeroAmounts() {
        when(customerRepository.count()).thenReturn(0L);
        when(paymentTransactionRepository.count()).thenReturn(0L);
        when(paymentTransactionRepository.countByPaymentStatus(PaymentStatus.COMPLETED)).thenReturn(0L);
        when(paymentTransactionRepository.countByPaymentStatus(PaymentStatus.FAILED)).thenReturn(0L);
        when(paymentTransactionRepository.sumAmountByStatus(PaymentStatus.COMPLETED)).thenReturn(null);

        SupportDashboardResponse response = supportService.getDashboard();

        assertEquals(BigDecimal.ZERO, response.getTotalCreditAmount());
        assertEquals(BigDecimal.ZERO, response.getTotalDebitAmount());
    }

    @Test
    @DisplayName("getAllTransactions should sort by createdTime descending")
    void getAllTransactions_shouldReturnNewestFirst() {
        PaymentTransaction oldTx = transaction("TXN-OLD", LocalDateTime.of(2026, 8, 4, 10, 0, 0));
        PaymentTransaction newTx = transaction("TXN-NEW", LocalDateTime.of(2026, 8, 4, 11, 0, 0));

        when(paymentTransactionRepository.findAll()).thenReturn(List.of(oldTx, newTx));
        when(transactionMapper.toTransactionResponse(newTx))
                .thenReturn(TransactionResponse.builder().transactionId("TXN-NEW").build());
        when(transactionMapper.toTransactionResponse(oldTx))
                .thenReturn(TransactionResponse.builder().transactionId("TXN-OLD").build());

        List<TransactionResponse> responses = supportService.getAllTransactions();

        assertEquals(2, responses.size());
        assertEquals("TXN-NEW", responses.get(0).getTransactionId());
        assertEquals("TXN-OLD", responses.get(1).getTransactionId());
    }

    @Test
    @DisplayName("getTransactionsByCustomer should merge sent+received and sort newest first")
    void getTransactionsByCustomer_whenAccountExists_shouldReturnSortedMergedResults() {
        Account account = new Account();
        account.setAccountNumber("100000000001");

        PaymentTransaction sentOlder = transaction("TXN-SENT-OLD", LocalDateTime.of(2026, 8, 4, 9, 0, 0));
        PaymentTransaction receivedNew = transaction("TXN-REC-NEW", LocalDateTime.of(2026, 8, 4, 12, 0, 0));

        when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(account));
        when(paymentTransactionRepository.findBySenderAccountAccountNumber("100000000001")).thenReturn(List.of(sentOlder));
        when(paymentTransactionRepository.findByReceiverAccountAccountNumber("100000000001")).thenReturn(List.of(receivedNew));

        when(transactionMapper.toTransactionResponse(receivedNew))
                .thenReturn(TransactionResponse.builder().transactionId("TXN-REC-NEW").build());
        when(transactionMapper.toTransactionResponse(sentOlder))
                .thenReturn(TransactionResponse.builder().transactionId("TXN-SENT-OLD").build());

        List<TransactionResponse> responses = supportService.getTransactionsByCustomer("100000000001");

        assertEquals(2, responses.size());
        assertEquals("TXN-REC-NEW", responses.get(0).getTransactionId());
        assertEquals("TXN-SENT-OLD", responses.get(1).getTransactionId());
        verify(accountRepository).findByAccountNumber("100000000001");
    }

    @Test
    @DisplayName("getTransactionsByCustomer should throw when account does not exist")
    void getTransactionsByCustomer_whenAccountMissing_shouldThrowAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("404")).thenReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> supportService.getTransactionsByCustomer("404"));

        assertEquals("Account not found with number: 404", ex.getMessage());
    }

    @Test
    @DisplayName("getTransactionsByStatus should filter and sort newest first")
    void getTransactionsByStatus_shouldReturnSortedResults() {
        PaymentTransaction oldTx = transaction("TXN-OLD", LocalDateTime.of(2026, 8, 4, 8, 0, 0));
        PaymentTransaction newTx = transaction("TXN-NEW", LocalDateTime.of(2026, 8, 4, 13, 0, 0));

        when(paymentTransactionRepository.findByPaymentStatus(PaymentStatus.COMPLETED)).thenReturn(List.of(oldTx, newTx));
        when(transactionMapper.toTransactionResponse(newTx))
                .thenReturn(TransactionResponse.builder().transactionId("TXN-NEW").build());
        when(transactionMapper.toTransactionResponse(oldTx))
                .thenReturn(TransactionResponse.builder().transactionId("TXN-OLD").build());

        List<TransactionResponse> responses = supportService.getTransactionsByStatus(PaymentStatus.COMPLETED);

        assertEquals(2, responses.size());
        assertEquals("TXN-NEW", responses.get(0).getTransactionId());
        assertEquals("TXN-OLD", responses.get(1).getTransactionId());
    }

    private PaymentTransaction transaction(String transactionId, LocalDateTime createdTime) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setCreatedTime(createdTime);
        return transaction;
    }
}

