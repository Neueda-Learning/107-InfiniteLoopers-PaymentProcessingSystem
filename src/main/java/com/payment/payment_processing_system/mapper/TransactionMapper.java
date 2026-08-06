package com.payment.payment_processing_system.mapper;

import com.payment.payment_processing_system.dto.TransactionResponse;
import com.payment.payment_processing_system.model.PaymentTransaction;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting PaymentTransaction entities to TransactionResponse DTO.
 */
@Component
public class TransactionMapper {

    /**
     * Convert a PaymentTransaction entity to TransactionResponse DTO.
     *
     * @param transaction the PaymentTransaction entity
     * @return the TransactionResponse DTO
     */
    public TransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .senderAccountNumber(transaction.getSenderAccount().getAccountNumber())
                .receiverAccountNumber(transaction.getReceiverAccount().getAccountNumber())
                .amount(transaction.getAmount())
                .senderCurrency(transaction.getSenderCurrency())
                .receiverCurrency(transaction.getReceiverCurrency())
                .exchangeRate(transaction.getExchangeRate())
                .transferCharge(transaction.getTransferCharge())
                .convertedAmount(transaction.getConvertedAmount())
                .description(transaction.getDescription())
                .paymentStatus(transaction.getPaymentStatus().toString())
                .createdTime(transaction.getCreatedTime())
                .validatedTime(transaction.getValidatedTime())
                .sentTime(transaction.getSentTime())
                .completedTime(transaction.getCompletedTime())
                .failedTime(transaction.getFailedTime())
                .failureReason(transaction.getFailureReason())
                .build();
    }
}

