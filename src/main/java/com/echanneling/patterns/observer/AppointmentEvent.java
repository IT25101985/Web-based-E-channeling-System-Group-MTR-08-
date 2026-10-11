package com.echanneling.patterns.observer;

import com.echanneling.entity.Appointment;
import com.echanneling.entity.Notification;

public record AppointmentEvent(Appointment appointment, Notification.NotificationType type, String key) {}
