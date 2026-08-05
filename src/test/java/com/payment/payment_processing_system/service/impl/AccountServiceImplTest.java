package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.dto.AccountResponse;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.mapper.AccountMapper;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.Customer;
import com.payment.payment_processing_system.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    @DisplayName("getAccountById should return mapped response when account exists")
    void getAccountById_whenExists_shouldReturnMappedResponse() {
        Customer customer = new Customer();
        customer.setCustomerName("Alice Johnson");
        customer.setEmail("alice@example.com");

        Account account = new Account();
        account.setId(1L);
        account.setAccountNumber("100000000001");
        account.setBankName("HDFC Bank");
        account.setIfscCode("HDFC0005678");
        account.setBalance(new BigDecimal("50000.00"));
        account.setCustomer(customer);

        AccountResponse response = AccountResponse.builder()
                .accountNumber("100000000001")
                .bankName("HDFC Bank")
                .ifscCode("HDFC0005678")
                .balance(new BigDecimal("50000.00"))
                .customerName("Alice Johnson")
                .email("alice@example.com")
                .currency(null)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountMapper.toAccountResponse(account)).thenReturn(response);

        AccountResponse result = accountService.getAccountById(1L);

        assertEquals("Alice Johnson", result.getCustomerName());
    }

    @Test
    @DisplayName("getAccountById should throw when account does not exist")
    void getAccountById_whenMissing_shouldThrowAccountNotFoundException() {
        when(accountRepository.findById(404L)).thenReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccountById(404L));

        assertEquals("Account not found with ID: 404", ex.getMessage());
    }
}

