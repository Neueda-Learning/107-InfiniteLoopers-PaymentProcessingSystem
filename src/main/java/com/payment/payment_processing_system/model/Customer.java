package com.payment.payment_processing_system.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

/**
 * Customer entity representing a customer in the payment processing system.
 */
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "accounts")
@ToString(exclude = "accounts")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Account> accounts;
}

