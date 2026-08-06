package com.payment.payment_processing_system.mapper;

import com.payment.payment_processing_system.dto.CustomerResponse;
import com.payment.payment_processing_system.enums.CurrencyType;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CustomerMapperTest {

    private final CustomerMapper customerMapper = new CustomerMapper();

    @Test
    @DisplayName("toCustomerResponse should map customer and account fields into DTO")
    void toCustomerResponse_whenAccountPresent_shouldMapAllFields() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCustomerName("Alice Johnson");
        customer.setEmail("alice.johnson@example.com");
        customer.setPhoneNumber("9876543210");

        Account account = new Account();
        account.setAccountNumber("100000000001");
        account.setIfscCode("SBIN0001234");
        account.setBankName("State Bank of India");
        account.setBalance(new BigDecimal("50000.00"));
        account.setCurrency(CurrencyType.INR);

        CustomerResponse response = customerMapper.toCustomerResponse(customer, account);

        assertNotNull(response);
        assertEquals(1L, response.getCustomerId());
        assertEquals("Alice Johnson", response.getCustomerName());
        assertEquals("alice.johnson@example.com", response.getEmail());
        assertEquals("9876543210", response.getPhoneNumber());
        assertEquals("100000000001", response.getAccountNumber());
        assertEquals("SBIN0001234", response.getIfscCode());
        assertEquals("State Bank of India", response.getBankName());
        assertEquals(new BigDecimal("50000.00"), response.getBalance());
        assertEquals(CurrencyType.INR, response.getCurrency());
    }

    @Test
    @DisplayName("toCustomerResponse should map customer fields even when account is null")
    void toCustomerResponse_whenAccountAbsent_shouldMapCustomerFieldsOnly() {
        Customer customer = new Customer();
        customer.setId(2L);
        customer.setCustomerName("Bob Smith");
        customer.setEmail("bob.smith@example.com");
        customer.setPhoneNumber("9876501234");

        CustomerResponse response = customerMapper.toCustomerResponse(customer, null);

        assertNotNull(response);
        assertEquals(2L, response.getCustomerId());
        assertEquals("Bob Smith", response.getCustomerName());
        assertEquals("bob.smith@example.com", response.getEmail());
        assertEquals("9876501234", response.getPhoneNumber());
        assertEquals(null, response.getAccountNumber());
        assertEquals(null, response.getIfscCode());
        assertEquals(null, response.getBankName());
        assertEquals(null, response.getBalance());
        assertEquals(null, response.getCurrency());
    }
}

