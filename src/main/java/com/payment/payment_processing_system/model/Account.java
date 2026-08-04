package com.payment.payment_processing_system.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Account entity representing a bank account in the payment processing system.
 */
@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"customer", "sentTransactions", "receivedTransactions"})
@ToString(exclude = {"customer", "sentTransactions", "receivedTransactions"})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "upi_pin", nullable = false)
    private String upiPin;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "senderAccount", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PaymentTransaction> sentTransactions;

    @OneToMany(mappedBy = "receiverAccount", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PaymentTransaction> receivedTransactions;
}

