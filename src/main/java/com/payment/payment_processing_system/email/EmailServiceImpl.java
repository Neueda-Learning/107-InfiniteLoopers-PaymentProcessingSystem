package com.payment.payment_processing_system.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Real email implementation using JavaMailSender.
 * Sends a notification email for high-value transactions (amount > ₹10,000).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendNotification(String toEmail, String senderName, String transactionId, String amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("High-Value Transaction Alert – " + transactionId);
            message.setText(
                    "Dear " + senderName + ",\n\n" +
                    "A high-value transaction of ₹" + amount + " has been successfully processed on your account.\n\n" +
                    "Transaction ID : " + transactionId + "\n" +
                    "Amount         : ₹" + amount + "\n\n" +
                    "If you did not initiate this transaction, please contact our support team immediately.\n\n" +
                    "Regards,\n" +
                    "Payment Processing System Team"
            );

            mailSender.send(message);
            log.info("Email notification sent to [{}] for transaction [{}]", toEmail, transactionId);

        } catch (Exception ex) {
            log.error("Failed to send email notification to [{}] for transaction [{}]: {}",
                    toEmail, transactionId, ex.getMessage(), ex);
            throw ex;
        }
    }
}
