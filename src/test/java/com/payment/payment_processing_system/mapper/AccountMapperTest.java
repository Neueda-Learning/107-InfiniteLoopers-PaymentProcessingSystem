package com.payment.payment_processing_system.mapper;

import com.payment.payment_processing_system.dto.AccountResponse;
import com.payment.payment_processing_system.dto.CustomerAccountResponse;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountMapperTest {

    private final AccountMapper accountMapper = new AccountMapper();

    @Test
    @DisplayName("toCustomerAccountResponse should map account fields without sensitive data")
    void toCustomerAccountResponse_shouldMapExpectedFields() {
        Account account = new Account();
        account.setId(12L);
        account.setAccountNumber("100000000012");
        account.setBankName("State Bank of India");
        account.setIfscCode("SBIN0001234");
        account.setBalance(new BigDecimal("75000.00"));
        account.setActive(true);
        account.setUpiPin("1234");

        CustomerAccountResponse response = accountMapper.toCustomerAccountResponse(account);

        assertNotNull(response);
        assertEquals(12L, response.accountId());
        assertEquals("100000000012", response.accountNumber());
        assertEquals("State Bank of India", response.bankName());
        assertEquals("SBIN0001234", response.ifscCode());
        assertEquals(new BigDecimal("75000.00"), response.balance());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("toAccountResponse should map sender account details without UPI PIN")
    void toAccountResponse_shouldMapExpectedFields() {
        Customer customer = new Customer();
        customer.setCustomerName("Alice Johnson");
        customer.setEmail("alice.johnson@example.com");

        Account account = new Account();
        account.setId(21L);
        account.setAccountNumber("100000000021");
        account.setBankName("HDFC Bank");
        account.setIfscCode("HDFC0005678");
        account.setBalance(new BigDecimal("15000.00"));
        account.setCustomer(customer);
        account.setUpiPin("1234");

        AccountResponse response = accountMapper.toAccountResponse(account);

        assertNotNull(response);
        assertEquals("100000000021", response.getAccountNumber());
        assertEquals("HDFC Bank", response.getBankName());
        assertEquals("HDFC0005678", response.getIfscCode());
        assertEquals(new BigDecimal("15000.00"), response.getBalance());
        assertEquals("Alice Johnson", response.getCustomerName());
        assertEquals("alice.johnson@example.com", response.getEmail());
        assertEquals(null, response.getCurrency());
    }
}

