package com.echanneling.strategy;

import org.springframework.stereotype.Component;

/**
 * Concrete Strategy for SMS Notifications
 */
@Component("smsNotificationStrategy")
public class SmsNotificationStrategy implements NotificationStrategy {

    @Override
    public void sendNotification(String recipient, String subject, String body) {
        System.out.println("SMS notification sent to " + recipient + ": " + body);
    }
}
