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
import com.payment.payment_processing_system.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of CustomerService.
 * Handles customer-related business logic and operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AccountMapper accountMapper;
    private final CustomerMapper customerMapper;
    private final TransactionMapper transactionMapper;

    @Override
    public List<CustomerListItemResponse> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(customer -> new CustomerListItemResponse(customer.getId(), customer.getCustomerName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerAccountResponse> getActiveAccountsByCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be a positive number");
        }

        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
        }

        return accountRepository.findByCustomerIdAndActiveTrueOrderByBankNameAscIdAsc(customerId)
                .stream()
                .map(accountMapper::toCustomerAccountResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerAccountResponse> getActiveAccountsByCustomerIdentifier(String customerName, String email, String phoneNumber) {
        String normalizedName = normalize(customerName);
        String normalizedEmail = normalize(email);
        String normalizedPhone = normalize(phoneNumber);

        int provided = countProvided(normalizedName, normalizedEmail, normalizedPhone);
        if (provided == 0) {
            throw new IllegalArgumentException("Provide at least one identifier: customerName, email, or phoneNumber");
        }

        Customer customer;
        // Priority: email > phone > name (use first available)
        if (normalizedEmail != null) {
            customer = customerRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + normalizedEmail));
        } else if (normalizedPhone != null) {
            customer = customerRepository.findByPhoneNumber(normalizedPhone)
                    .orElseThrow(() -> new CustomerNotFoundException("Customer not found with phone number: " + normalizedPhone));
        } else {
            List<Customer> matches = customerRepository.findByCustomerNameIgnoreCase(normalizedName);
            if (matches.isEmpty()) {
                throw new CustomerNotFoundException("Customer not found with name: " + normalizedName);
            }
            if (matches.size() > 1) {
                throw new IllegalArgumentException("Multiple customers found for name. Use email or phoneNumber.");
            }
            customer = matches.get(0);
        }

        return getActiveAccountsByCustomerId(customer.getId());
    }

    @Override
    public CustomerResponse getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        // Fetch the first account associated with the customer (if any)
        List<Account> accounts = customer.getAccounts() != null ? 
            new ArrayList<>(customer.getAccounts()) : new ArrayList<>();
        Account firstAccount = accounts.isEmpty() ? null : accounts.get(0);

        return customerMapper.toCustomerResponse(customer, firstAccount);
    }

    @Override
    public CustomerResponse getCustomerByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with number: " + accountNumber));

        Customer customer = account.getCustomer();
        return customerMapper.toCustomerResponse(customer, account);
    }

    @Override
    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with number: " + accountNumber));

        // Fetch transactions where the account is sender
        List<PaymentTransaction> sentTransactions = paymentTransactionRepository
                .findBySenderAccountAccountNumber(accountNumber);

        // Fetch transactions where the account is receiver
        List<PaymentTransaction> receivedTransactions = paymentTransactionRepository
                .findByReceiverAccountAccountNumber(accountNumber);

        // Combine both lists
        List<PaymentTransaction> allTransactions = new ArrayList<>(sentTransactions);
        allTransactions.addAll(receivedTransactions);

        // Sort by createdTime descending (newest first)
        return allTransactions.stream()
                .sorted((t1, t2) -> t2.getCreatedTime().compareTo(t1.getCreatedTime()))
                .map(transactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionResponse getTransactionDetails(String transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with ID: " + transactionId));

        return transactionMapper.toTransactionResponse(transaction);
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int countProvided(String customerName, String email, String phoneNumber) {
        int count = 0;
        if (customerName != null) {
            count++;
        }
        if (email != null) {
            count++;
        }
        if (phoneNumber != null) {
            count++;
        }
        return count;
    }
}

