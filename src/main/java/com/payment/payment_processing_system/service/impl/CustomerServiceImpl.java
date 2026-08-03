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
    private final CustomerMapper customerMapper;
    private final TransactionMapper transactionMapper;

    @Override
    public List<CustomerResponse> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(customer -> {
                    // Fetch the first account associated with the customer (if any)
                    List<Account> accounts = customer.getAccounts() != null ? 
                        new ArrayList<>(customer.getAccounts()) : new ArrayList<>();
                    Account firstAccount = accounts.isEmpty() ? null : accounts.get(0);
                    return customerMapper.toCustomerResponse(customer, firstAccount);
                })
                .collect(Collectors.toList());
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
        Account account = accountRepository.findByAccountNumber(accountNumber)
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
}

