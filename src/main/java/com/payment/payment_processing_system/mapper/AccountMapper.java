package com.payment.payment_processing_system.mapper;

import com.payment.payment_processing_system.dto.AccountResponse;
import com.payment.payment_processing_system.dto.CustomerAccountResponse;
import com.payment.payment_processing_system.model.Account;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting Account entities to customer account DTOs.
 */
@Component
public class AccountMapper {

    /**
     * Convert an Account entity into CustomerAccountResponse.
     *
     * @param account account entity
     * @return mapped DTO without sensitive fields
     */
    public CustomerAccountResponse toCustomerAccountResponse(Account account) {
        return new CustomerAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBankName(),
                account.getIfscCode(),
                account.getBalance(),
                account.isActive()
        );
    }

    /**
     * Convert an Account entity into safe account details for sender lookup.
     *
     * @param account account entity
     * @return safe account response without UPI PIN
     */
    public AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .bankName(account.getBankName())
                .ifscCode(account.getIfscCode())
                .balance(account.getBalance())
                .customerName(account.getCustomer() != null ? account.getCustomer().getCustomerName() : null)
                .email(account.getCustomer() != null ? account.getCustomer().getEmail() : null)
                .currency(null)
                .build();
    }
}

