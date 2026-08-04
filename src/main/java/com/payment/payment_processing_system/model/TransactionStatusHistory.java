package com.payment.payment_processing_system.model;

import jakarta.persistence.*;
import lombok.*;
import com.payment.payment_processing_system.enums.PaymentStatus;
import java.time.LocalDateTime;

/**
 * TransactionStatusHistory entity for auditing the status changes of payment transactions.
 */
@Entity
@Table(name = "transaction_status_history", indexes = {
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "transaction")
@ToString(exclude = "transaction")
public class TransactionStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private PaymentTransaction transaction;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreated() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

