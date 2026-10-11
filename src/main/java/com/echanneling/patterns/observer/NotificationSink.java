package com.echanneling.patterns.observer;

import com.echanneling.entity.User;

@FunctionalInterface
public interface NotificationSink {
    void queue(AppointmentEvent event, User recipient, String email, String eventKey);
}
