package com.echanneling.strategy;

/**
 * Strategy Pattern Interface for Notifications
 * Demonstrates Strategy Pattern for multi-channel messaging (Email, SMS).
 */
public interface NotificationStrategy {
    void sendNotification(String recipient, String subject, String body);
}
