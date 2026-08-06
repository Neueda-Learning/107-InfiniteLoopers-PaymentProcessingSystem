package com.payment.payment_processing_system.repository;

import com.payment.payment_processing_system.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Customer entity.
 * Provides database operations for Customer records.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find a customer by email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the Customer if found, empty otherwise
     */
    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    List<Customer> findByCustomerNameIgnoreCase(String customerName);
}

