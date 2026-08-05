package com.payment.payment_processing_system.mapper;

import com.payment.payment_processing_system.dto.CustomerResponse;
import com.payment.payment_processing_system.model.Account;
import com.payment.payment_processing_system.model.Customer;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting Customer and Account entities to CustomerResponse DTO.
 */
@Component
public class CustomerMapper {

    /**
     * Convert a Customer entity and optional Account to CustomerResponse DTO.
     *
     * @param customer the Customer entity
     * @param account the Account entity (optional)
     * @return the CustomerResponse DTO
     */
    public CustomerResponse toCustomerResponse(Customer customer, Account account) {
        CustomerResponse response = CustomerResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getCustomerName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .build();

        if (account != null) {
            response.setAccountNumber(account.getAccountNumber());
            response.setIfscCode(account.getIfscCode());
            response.setBankName(account.getBankName());
            response.setBalance(account.getBalance());
            response.setCurrency(account.getCurrency());
        }

        return response;
    }
}

