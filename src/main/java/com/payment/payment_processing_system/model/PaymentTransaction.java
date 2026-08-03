package com.payment.payment_processing_system.model;

import jakarta.persistence.*;
import lombok.*;
import com.payment.payment_processing_system.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PaymentTransaction entity representing a payment transaction in the system.
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_transaction_id", columnList = "transaction_id", unique = true),
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_payment_status", columnList = "payment_status"),
    @Index(name = "idx_created_time", columnList = "created_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "statusHistory")
@ToString(exclude = "statusHistory")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 50)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_account_id", nullable = false)
    private Account senderAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_account_id", nullable = false)
    private Account receiverAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = true, length = 500)
    private String description;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "validated_time")
    private LocalDateTime validatedTime;

    @Column(name = "sent_time")
    private LocalDateTime sentTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    @Column(name = "failed_time")
    private LocalDateTime failedTime;

    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TransactionStatusHistory> statusHistory;

    @PrePersist
    protected void onCreated() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.CREATED;
        }
    }
}

