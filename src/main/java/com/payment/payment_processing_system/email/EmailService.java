package com.payment.payment_processing_system.email;

/**
 * Service interface for sending email notifications.
 */
public interface EmailService {

    /**
     * Send a high-value transaction notification email to the sender.
     *
     * @param toEmail     the recipient email address
     * @param senderName  the name of the sender
     * @param transactionId the unique transaction ID
     * @param amount      the transaction amount as a string
     */
    void sendNotification(String toEmail, String senderName, String transactionId, String amount);
}

