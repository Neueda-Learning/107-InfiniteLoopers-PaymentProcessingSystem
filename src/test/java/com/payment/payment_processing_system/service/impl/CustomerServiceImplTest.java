package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.dto.CustomerResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.exception.CustomerNotFoundException;
import com.payment.payment_processing_system.exception.TransactionNotFoundException;
import com.payment.payment_processing_system.mapper.CustomerMapper;
import com.payment.payment_processing_system.mapper.TransactionMapper;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.Customer;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    @DisplayName("getAllCustomers should map each customer using first account if available")
    void getAllCustomers_shouldMapUsingFirstAccount() {
        Customer customerWithAccount = customer(1L, "Alice");
        Account firstAccount = account("100000000001");
        Account secondAccount = account("100000000099");
        customerWithAccount.setAccounts(Arrays.asList(firstAccount, secondAccount));

        Customer customerWithoutAccount = customer(2L, "Bob");
        customerWithoutAccount.setAccounts(null);

        CustomerResponse response1 = CustomerResponse.builder().customerId(1L).customerName("Alice").build();
        CustomerResponse response2 = CustomerResponse.builder().customerId(2L).customerName("Bob").build();

        when(customerRepository.findAll()).thenReturn(List.of(customerWithAccount, customerWithoutAccount));
        when(customerMapper.toCustomerResponse(customerWithAccount, firstAccount)).thenReturn(response1);
        when(customerMapper.toCustomerResponse(customerWithoutAccount, null)).thenReturn(response2);

        List<CustomerResponse> responses = customerService.getAllCustomers();

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getCustomerId());
        assertEquals(2L, responses.get(1).getCustomerId());
        verify(customerMapper).toCustomerResponse(customerWithAccount, firstAccount);
        verify(customerMapper).toCustomerResponse(customerWithoutAccount, null);
    }

    @Test
    @DisplayName("getCustomerById should return mapped response when customer exists")
    void getCustomerById_whenExists_shouldReturnMappedResponse() {
        Customer customer = customer(1L, "Alice");
        Account account = account("100000000001");
        customer.setAccounts(List.of(account));

        CustomerResponse response = CustomerResponse.builder().customerId(1L).customerName("Alice").build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerMapper.toCustomerResponse(customer, account)).thenReturn(response);

        CustomerResponse result = customerService.getCustomerById(1L);

        assertEquals(1L, result.getCustomerId());
        verify(customerMapper).toCustomerResponse(customer, account);
    }

    @Test
    @DisplayName("getCustomerById should throw when customer does not exist")
    void getCustomerById_whenMissing_shouldThrowCustomerNotFoundException() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        CustomerNotFoundException ex = assertThrows(CustomerNotFoundException.class,
                () -> customerService.getCustomerById(99L));

        assertEquals("Customer not found with ID: 99", ex.getMessage());
    }

    @Test
    @DisplayName("getCustomerByAccountNumber should map using account and customer")
    void getCustomerByAccountNumber_whenExists_shouldReturnMappedResponse() {
        Customer customer = customer(3L, "Charlie");
        Account account = account("100000000003");
        account.setCustomer(customer);

        CustomerResponse response = CustomerResponse.builder().customerId(3L).customerName("Charlie").build();

        when(accountRepository.findByAccountNumber("100000000003")).thenReturn(Optional.of(account));
        when(customerMapper.toCustomerResponse(customer, account)).thenReturn(response);

        CustomerResponse result = customerService.getCustomerByAccountNumber("100000000003");

        assertEquals(3L, result.getCustomerId());
        verify(customerMapper).toCustomerResponse(customer, account);
    }

    @Test
    @DisplayName("getCustomerByAccountNumber should throw when account does not exist")
    void getCustomerByAccountNumber_whenMissing_shouldThrowAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("000")).thenReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> customerService.getCustomerByAccountNumber("000"));

        assertEquals("Account not found with number: 000", ex.getMessage());
    }

    @Test
    @DisplayName("getTransactionHistory should combine sent and received and sort newest first")
    void getTransactionHistory_shouldCombineAndSortDescending() {
        Account account = account("100000000001");
        when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(account));

        PaymentTransaction sentOlder = transaction("TXN-SENT-OLD", LocalDateTime.of(2026, 8, 4, 10, 0, 0));
        PaymentTransaction sentNewest = transaction("TXN-SENT-NEW", LocalDateTime.of(2026, 8, 4, 12, 0, 0));
        PaymentTransaction receivedMiddle = transaction("TXN-REC-MID", LocalDateTime.of(2026, 8, 4, 11, 0, 0));

        when(paymentTransactionRepository.findBySenderAccountAccountNumber("100000000001"))
                .thenReturn(List.of(sentOlder, sentNewest));
        when(paymentTransactionRepository.findByReceiverAccountAccountNumber("100000000001"))
                .thenReturn(List.of(receivedMiddle));

        when(transactionMapper.toTransactionResponse(sentNewest))
                .thenReturn(TransactionResponse.builder().transactionId("TXN-SENT-NEW").build());
        when(transactionMapper.toTransactionResponse(receivedMiddle))
                .thenReturn(TransactionResponse.builder().transactionId("TXN-REC-MID").build());
        when(transactionMapper.toTransactionResponse(sentOlder))
                .thenReturn(TransactionResponse.builder().transactionId("TXN-SENT-OLD").build());

        List<TransactionResponse> responses = customerService.getTransactionHistory("100000000001");

        assertEquals(3, responses.size());
        assertEquals("TXN-SENT-NEW", responses.get(0).getTransactionId());
        assertEquals("TXN-REC-MID", responses.get(1).getTransactionId());
        assertEquals("TXN-SENT-OLD", responses.get(2).getTransactionId());
    }

    @Test
    @DisplayName("getTransactionHistory should throw when account does not exist")
    void getTransactionHistory_whenAccountMissing_shouldThrowAccountNotFoundException() {
        when(accountRepository.findByAccountNumber("404")).thenReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> customerService.getTransactionHistory("404"));

        assertEquals("Account not found with number: 404", ex.getMessage());
    }

    @Test
    @DisplayName("getTransactionDetails should map transaction when found")
    void getTransactionDetails_whenFound_shouldReturnMappedResponse() {
        PaymentTransaction transaction = transaction("TXN-100", LocalDateTime.of(2026, 8, 4, 10, 0, 0));
        TransactionResponse response = TransactionResponse.builder().transactionId("TXN-100").build();

        when(paymentTransactionRepository.findByTransactionId("TXN-100")).thenReturn(Optional.of(transaction));
        when(transactionMapper.toTransactionResponse(transaction)).thenReturn(response);

        TransactionResponse result = customerService.getTransactionDetails("TXN-100");

        assertEquals("TXN-100", result.getTransactionId());
    }

    @Test
    @DisplayName("getTransactionDetails should throw when transaction does not exist")
    void getTransactionDetails_whenMissing_shouldThrowTransactionNotFoundException() {
        when(paymentTransactionRepository.findByTransactionId("TXN-404")).thenReturn(Optional.empty());

        TransactionNotFoundException ex = assertThrows(TransactionNotFoundException.class,
                () -> customerService.getTransactionDetails("TXN-404"));

        assertEquals("Transaction not found with ID: TXN-404", ex.getMessage());
    }

    private Customer customer(Long id, String name) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCustomerName(name);
        customer.setEmail(name.toLowerCase() + "@example.com");
        customer.setPhoneNumber("9999999999");
        return customer;
    }

    private Account account(String accountNumber) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setIfscCode("HDFC0005678");
        account.setBankName("HDFC Bank");
        return account;
    }

    private PaymentTransaction transaction(String transactionId, LocalDateTime createdTime) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setCreatedTime(createdTime);
        return transaction;
    }
}

