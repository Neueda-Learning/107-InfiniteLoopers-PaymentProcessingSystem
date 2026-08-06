package com.payment.payment_processing_system.service.impl;

import com.payment.payment_processing_system.dto.AccountResponse;
import com.payment.payment_processing_system.exception.AccountNotFoundException;
import com.payment.payment_processing_system.mapper.AccountMapper;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.repository.AccountRepository;
import com.payment.payment_processing_system.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of account read operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    public AccountResponse getAccountById(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("Account ID must be a positive number");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        return accountMapper.toAccountResponse(account);
    }
}

