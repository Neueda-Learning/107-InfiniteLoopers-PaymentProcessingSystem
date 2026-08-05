package com.payment.payment_processing_system.service;

import com.payment.payment_processing_system.dto.AccountResponse;

/**
 * Service interface for account read operations.
 */
public interface AccountService {

    /**
     * Retrieve a sender account by ID.
     *
     * @param accountId account ID
     * @return safe account details
     */
    AccountResponse getAccountById(Long accountId);
}

