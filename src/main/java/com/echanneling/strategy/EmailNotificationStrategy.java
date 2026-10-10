package com.echanneling.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Transport errors propagate to the outbox, which controls retries and delivery status. */
@Component("emailNotificationStrategy")
public class EmailNotificationStrategy implements NotificationStrategy {
    private final JavaMailSender mail;
    private final String from;
    public EmailNotificationStrategy(JavaMailSender mail, @Value("${app.mail.from:}") String from) {
        this.mail=mail; this.from=from;
    }
    public void sendNotification(String recipient, String subject, String body) {
        if(from == null || !from.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Sender is not configured.");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(recipient); message.setSubject(subject); message.setText(body);
        mail.send(message);
    }
}
