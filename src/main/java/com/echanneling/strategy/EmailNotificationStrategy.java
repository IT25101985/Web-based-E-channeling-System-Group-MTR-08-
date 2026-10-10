package com.echanneling.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Concrete Strategy for Email Notifications
 */
@Component("emailNotificationStrategy")
public class EmailNotificationStrategy implements NotificationStrategy {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public void sendNotification(String recipient, String subject, String body) {
        try {
            if (mailSender != null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("noreply@healthcareplus.com");
                message.setTo(recipient);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                System.out.println("Email notification sent to: " + recipient);
            } else {
                System.out.println("Mock Email notification to " + recipient + ": [" + subject + "] " + body);
            }
        } catch (Exception e) {
            System.out.println("Mock Email fallback sent to " + recipient + " (Mail server offline): [" + subject + "]");
        }
    }
}
