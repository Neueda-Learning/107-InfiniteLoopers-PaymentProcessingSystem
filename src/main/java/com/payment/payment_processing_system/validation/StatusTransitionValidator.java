package com.payment.payment_processing_system.validation;

import com.payment.payment_processing_system.enums.PaymentStatus;
import com.payment.payment_processing_system.exception.InvalidPaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates payment status transitions against the defined state machine.
 *
 * Valid transitions:
 *   CREATED    → VALIDATED, FAILED
 *   VALIDATED  → SENT,      FAILED
 *   SENT       → COMPLETED, FAILED
 *   FAILED     → CREATED   (retry path only)
 *
 * Any transition not in this map is rejected with a 400 error.
 */
@Slf4j
@Component
public class StatusTransitionValidator {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PaymentStatus.CREATED,   EnumSet.of(PaymentStatus.VALIDATED, PaymentStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.VALIDATED, EnumSet.of(PaymentStatus.SENT,      PaymentStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.SENT,      EnumSet.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.FAILED,    EnumSet.of(PaymentStatus.CREATED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.COMPLETED, EnumSet.noneOf(PaymentStatus.class));
    }

    /**
     * Validates that transitioning from {@code current} to {@code target} is permitted.
     *
     * @param transactionId the transaction ID — used only for the error message
     * @param current       the current status of the transaction
     * @param target        the desired next status
     * @throws InvalidPaymentException if the transition is not allowed
     */
    public void validate(String transactionId, PaymentStatus current, PaymentStatus target) {
        Set<PaymentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(PaymentStatus.class));

        if (!allowed.contains(target)) {
            String message = String.format(
                    "Invalid status transition for transaction [%s]: %s → %s is not allowed. " +
                    "Allowed transitions from %s: %s",
                    transactionId, current, target, current,
                    allowed.isEmpty() ? "none (terminal state)" : allowed.toString());

            log.warn(message);
            throw new InvalidPaymentException(message);
        }

        log.debug("Status transition valid for transaction [{}]: {} → {}", transactionId, current, target);
    }
}

