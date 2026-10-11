package com.echanneling.patterns.decorator;

import com.echanneling.entity.Appointment;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Adds appointment details while preserving the wrapped component's behavior. */
public final class AppointmentMessageDecorator extends MessageDecorator {
    private final Appointment appointment;
    public AppointmentMessageDecorator(NotificationMessage wrapped, Appointment appointment) {
        super(wrapped); this.appointment = appointment;
    }
    public String render() {
        return wrapped.render().replace("{patient}", Objects.toString(appointment.getPatientName(), "Patient"))
            .replace("{doctor}", appointment.getDoctor().getName())
            .replace("{hospital}", appointment.getHospitalBranch() == null ? "your hospital" : appointment.getHospitalBranch().getBranchName())
            .replace("{date}", appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")))
            .replace("{reference}", appointment.getId().toString());
    }
}
