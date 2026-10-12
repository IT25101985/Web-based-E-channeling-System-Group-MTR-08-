package com.echanneling.strategy;

import org.springframework.stereotype.Component;

/**
 * Concrete Strategy for SMS Notifications
 */
@Component("smsNotificationStrategy")
public class SmsNotificationStrategy implements NotificationStrategy {

    @Override
    public void sendNotification(String recipient, String subject, String body) {
        throw new UnsupportedOperationException("SMS delivery is not configured. Use the portal notification inbox.");
    }
}
