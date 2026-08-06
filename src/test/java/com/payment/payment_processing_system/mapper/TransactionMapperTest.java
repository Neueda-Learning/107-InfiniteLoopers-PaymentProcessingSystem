package com.payment.payment_processing_system.mapper;

import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.enums.CurrencyType;
import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.Customer;
import com.payment.payment_processing_system.model.PaymentTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransactionMapperTest {

    private final TransactionMapper transactionMapper = new TransactionMapper();

    @Test
    @DisplayName("toTransactionResponse should map transaction fields into DTO")
    void toTransactionResponse_shouldMapAllFields() {
        Customer senderCustomer = new Customer();
        senderCustomer.setId(1L);
        senderCustomer.setCustomerName("Alice Johnson");
        senderCustomer.setEmail("alice.johnson@example.com");
        senderCustomer.setPhoneNumber("9876543210");

        Customer receiverCustomer = new Customer();
        receiverCustomer.setId(2L);
        receiverCustomer.setCustomerName("Bob Smith");
        receiverCustomer.setEmail("bob.smith@example.com");
        receiverCustomer.setPhoneNumber("9876501234");

        Account senderAccount = new Account();
        senderAccount.setAccountNumber("100000000001");
        senderAccount.setCustomer(senderCustomer);

        Account receiverAccount = new Account();
        receiverAccount.setAccountNumber("100000000002");
        receiverAccount.setCustomer(receiverCustomer);

        LocalDateTime createdTime = LocalDateTime.of(2026, 8, 4, 10, 0, 0);
        LocalDateTime validatedTime = LocalDateTime.of(2026, 8, 4, 10, 0, 5);
        LocalDateTime sentTime = LocalDateTime.of(2026, 8, 4, 10, 0, 7);
        LocalDateTime completedTime = LocalDateTime.of(2026, 8, 4, 10, 0, 10);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionId("TXN-0001")
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .amount(new BigDecimal("1500.00"))
                .senderCurrency(CurrencyType.USD)
                .receiverCurrency(CurrencyType.INR)
                .exchangeRate(new BigDecimal("87"))
                .transferCharge(new BigDecimal("30.0000"))
                .convertedAmount(new BigDecimal("130500.0000"))
                .description("Salary payment")
                .paymentStatus(PaymentStatus.COMPLETED)
                .createdTime(createdTime)
                .validatedTime(validatedTime)
                .sentTime(sentTime)
                .completedTime(completedTime)
                .failedTime(null)
                .build();

        TransactionResponse response = transactionMapper.toTransactionResponse(transaction);

        assertNotNull(response);
        assertEquals("TXN-0001", response.getTransactionId());
        assertEquals("100000000001", response.getSenderAccountNumber());
        assertEquals("100000000002", response.getReceiverAccountNumber());
        assertEquals(new BigDecimal("1500.00"), response.getAmount());
        assertEquals(CurrencyType.USD, response.getSenderCurrency());
        assertEquals(CurrencyType.INR, response.getReceiverCurrency());
        assertEquals(new BigDecimal("87"), response.getExchangeRate());
        assertEquals(new BigDecimal("30.0000"), response.getTransferCharge());
        assertEquals(new BigDecimal("130500.0000"), response.getConvertedAmount());
        assertEquals("Salary payment", response.getDescription());
        assertEquals("COMPLETED", response.getPaymentStatus());
        assertEquals(createdTime, response.getCreatedTime());
        assertEquals(validatedTime, response.getValidatedTime());
        assertEquals(sentTime, response.getSentTime());
        assertEquals(completedTime, response.getCompletedTime());
        assertEquals(null, response.getFailedTime());
    }
}

