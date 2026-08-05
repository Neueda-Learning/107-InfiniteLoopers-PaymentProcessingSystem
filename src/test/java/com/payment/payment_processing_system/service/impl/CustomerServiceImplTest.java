package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.dto.CustomerAccountResponse;
import com.payment.payment_processing_system.dto.CustomerListItemResponse;
import com.payment.payment_processing_system.dto.CustomerResponse;
import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.exception.CustomerNotFoundException;
import com.payment.payment_processing_system.exception.TransactionNotFoundException;
import com.payment.payment_processing_system.mapper.AccountMapper;
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
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private AccountMapper accountMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    @DisplayName("getAllCustomers should return only customer selection fields")
    void getAllCustomers_shouldReturnSelectionFields() {
        Customer customerWithAccount = customer(1L, "Alice");
        Account firstAccount = account("100000000001");
        Account secondAccount = account("100000000099");
        customerWithAccount.setAccounts(Arrays.asList(firstAccount, secondAccount));

        Customer customerWithoutAccount = customer(2L, "Bob");
        customerWithoutAccount.setAccounts(null);

        when(customerRepository.findAll()).thenReturn(List.of(customerWithAccount, customerWithoutAccount));

        List<CustomerListItemResponse> responses = customerService.getAllCustomers();

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals("Alice", responses.get(0).customerName());
        assertEquals(2L, responses.get(1).id());
        assertEquals("Bob", responses.get(1).customerName());
    }

    @Test
    @DisplayName("getActiveAccountsByCustomerId should return active accounts only")
    void getActiveAccountsByCustomerId_shouldReturnActiveAccounts() {
        Account active1 = account("100000000001");
        active1.setId(10L);
        active1.setBalance(new BigDecimal("50000.0000"));
        active1.setActive(true);

        Account active2 = account("100000000002");
        active2.setId(11L);
        active2.setBalance(new BigDecimal("30000.0000"));
        active2.setActive(true);

        CustomerAccountResponse mapped1 = new CustomerAccountResponse(
                10L,
                "100000000001",
                "Axis Bank",
                "HDFC0005678",
                new BigDecimal("50000.0000"),
                true
        );
        CustomerAccountResponse mapped2 = new CustomerAccountResponse(
                11L,
                "100000000002",
                "HDFC Bank",
                "HDFC0005678",
                new BigDecimal("30000.0000"),
                true
        );

        when(customerRepository.existsById(1L)).thenReturn(true);
        when(accountRepository.findByCustomerIdAndActiveTrueOrderByBankNameAscIdAsc(1L)).thenReturn(List.of(active1, active2));
        when(accountMapper.toCustomerAccountResponse(active1)).thenReturn(mapped1);
        when(accountMapper.toCustomerAccountResponse(active2)).thenReturn(mapped2);

        List<CustomerAccountResponse> responses = customerService.getActiveAccountsByCustomerId(1L);

        assertEquals(2, responses.size());
        assertEquals(10L, responses.get(0).accountId());
        assertEquals("100000000001", responses.get(0).accountNumber());
        assertTrue(responses.get(0).isActive());
        assertEquals("Axis Bank", responses.get(0).bankName());
        assertEquals("HDFC Bank", responses.get(1).bankName());
    }

    @Test
    @DisplayName("getActiveAccountsByCustomerId should throw when customer does not exist")
    void getActiveAccountsByCustomerId_whenMissing_shouldThrowCustomerNotFoundException() {
        when(customerRepository.existsById(99L)).thenReturn(false);

        CustomerNotFoundException ex = assertThrows(CustomerNotFoundException.class,
                () -> customerService.getActiveAccountsByCustomerId(99L));

        assertEquals("Customer not found with ID: 99", ex.getMessage());
    }

    @Test
    @DisplayName("getActiveAccountsByCustomerIdentifier should return active accounts by email")
    void getActiveAccountsByCustomerIdentifier_byEmail_shouldReturnActiveAccounts() {
        Customer customer = customer(1L, "Alice");

        Account active = account("100000000001");
        active.setId(10L);
        active.setBalance(new BigDecimal("50000.0000"));
        active.setActive(true);

        CustomerAccountResponse mapped = new CustomerAccountResponse(
                10L,
                "100000000001",
                "HDFC Bank",
                "HDFC0005678",
                new BigDecimal("50000.0000"),
                true
        );

        when(customerRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(customerRepository.existsById(1L)).thenReturn(true);
        when(accountRepository.findByCustomerIdAndActiveTrueOrderByBankNameAscIdAsc(1L)).thenReturn(List.of(active));
        when(accountMapper.toCustomerAccountResponse(active)).thenReturn(mapped);

        List<CustomerAccountResponse> responses = customerService
                .getActiveAccountsByCustomerIdentifier(null, "alice@example.com", null);

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).accountId());
    }

    @Test
    @DisplayName("getActiveAccountsByCustomerIdentifier should throw when no identifier is provided")
    void getActiveAccountsByCustomerIdentifier_whenNoIdentifier_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> customerService.getActiveAccountsByCustomerIdentifier(null, null, null));

        assertEquals("Provide at least one identifier: customerName, email, or phoneNumber", ex.getMessage());
    }

    @Test
    @DisplayName("getActiveAccountsByCustomerIdentifier should use email when both email and name provided (priority)")
    void getActiveAccountsByCustomerIdentifier_withMultipleIdentifiers_shouldUsePriority() {
        Customer customer = customer(1L, "Alice");

        Account active = account("100000000001");
        active.setId(10L);
        active.setBalance(new BigDecimal("50000.0000"));
        active.setActive(true);

        CustomerAccountResponse mapped = new CustomerAccountResponse(
                10L,
                "100000000001",
                "HDFC Bank",
                "HDFC0005678",
                new BigDecimal("50000.0000"),
                true
        );

        when(customerRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(customer));
        when(customerRepository.existsById(1L)).thenReturn(true);
        when(accountRepository.findByCustomerIdAndActiveTrueOrderByBankNameAscIdAsc(1L)).thenReturn(List.of(active));
        when(accountMapper.toCustomerAccountResponse(active)).thenReturn(mapped);

        List<CustomerAccountResponse> responses = customerService
                .getActiveAccountsByCustomerIdentifier("SomeName", "alice@example.com", "9999999999");

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).accountId());
    }

    @Test
    @DisplayName("getActiveAccountsByCustomerIdentifier should throw when customer email is missing")
    void getActiveAccountsByCustomerIdentifier_whenEmailMissing_shouldThrowCustomerNotFoundException() {
        when(customerRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        CustomerNotFoundException ex = assertThrows(CustomerNotFoundException.class,
                () -> customerService.getActiveAccountsByCustomerIdentifier(null, "missing@example.com", null));

        assertEquals("Customer not found with email: missing@example.com", ex.getMessage());
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

