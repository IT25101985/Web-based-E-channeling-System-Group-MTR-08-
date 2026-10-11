package com.echanneling.patterns.observer;

public final class DoctorNotificationObserver implements AppointmentObserver {
    private final NotificationSink sink;
    public DoctorNotificationObserver(NotificationSink sink) { this.sink = sink; }
    public void update(AppointmentEvent event) {
        var doctor = event.appointment().getDoctor().getUser();
        if (doctor != null) sink.queue(event, doctor, doctor.getEmail(), event.key() + ":doctor");
    }
}
