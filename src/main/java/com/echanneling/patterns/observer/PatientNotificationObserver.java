package com.echanneling.patterns.observer;

public final class PatientNotificationObserver implements AppointmentObserver {
    private final NotificationSink sink;
    public PatientNotificationObserver(NotificationSink sink) { this.sink = sink; }
    public void update(AppointmentEvent event) {
        var appointment = event.appointment();
        sink.queue(event, appointment.getPatient(), appointment.getContactEmail(), event.key() + ":patient");
    }
}
