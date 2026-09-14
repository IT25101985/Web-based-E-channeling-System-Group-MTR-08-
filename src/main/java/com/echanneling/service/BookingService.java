package com.echanneling.service;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** One booking policy shared by the availability API and appointment writes. */
@Service
public class BookingService {
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final NotificationRepository notifications;
    public BookingService(DoctorRepository doctors, AppointmentRepository appointments, NotificationRepository notifications) {
        this.doctors = doctors;
        this.appointments = appointments;
        this.notifications = notifications;
    }

    public List<String> availableSlots(Long doctorId, LocalDate date, Long excludeId) {
        Doctor doctor = doctors.findById(doctorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (date.isBefore(LocalDate.now()) || date.getDayOfWeek() == DayOfWeek.SUNDAY) return List.of();
        List<Appointment> booked = appointments.findByDoctorAndAppointmentDateBetween(doctor, date.atStartOfDay(), date.atTime(LocalTime.MAX));
        LocalTime start = LocalTime.of(9, 0), end = LocalTime.of(17, 0);
        if (doctor.getClinicHours() != null) {
            try {
                String[] hours = doctor.getClinicHours().split("\\s+-\\s+");
                DateTimeFormatter format = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
                start = LocalTime.parse(hours[0].trim().toUpperCase(Locale.ROOT), format);
                end = LocalTime.parse(hours[1].trim().toUpperCase(Locale.ROOT), format);
            } catch (RuntimeException ignored) { /* Legacy free text uses the documented default clinic hours. */ }
        }
        List<String> slots = new ArrayList<>();
        for (LocalDateTime time = date.atTime(start); time.isBefore(date.atTime(end)); time = time.plusHours(1)) {
            LocalDateTime slot = time;
            if (slot.isAfter(LocalDateTime.now()) && booked.stream().noneMatch(a ->
                    !Objects.equals(a.getId(), excludeId) && !"CANCELLED".equals(a.getStatus()) && slot.equals(a.getAppointmentDate()))) {
                slots.add(slot.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            }
        }
        return slots;
    }

    @Transactional
    public Appointment book(User patient, Appointment submitted) {
        if (submitted.getDoctor() == null || submitted.getDoctor().getId() == null || submitted.getAppointmentDate() == null)
            throw new IllegalArgumentException("Choose a doctor, date and available time.");
        // Lock the doctor's database row until commit, including concurrent requests on other app instances.
        Doctor doctor = doctors.findForBooking(submitted.getDoctor().getId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor is no longer available."));
        Appointment booking = new Appointment();
        if (submitted.getId() != null) {
            booking = appointments.findById(submitted.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (booking.getPatient() == null || !Objects.equals(booking.getPatient().getId(), patient.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            if (!"SCHEDULED".equals(booking.getStatus())) throw new IllegalArgumentException("Only scheduled appointments can be rescheduled.");
            if (!Objects.equals(booking.getDoctor().getId(), doctor.getId()))
                throw new IllegalArgumentException("Keep the same doctor when rescheduling. Cancel and make a new booking to change doctors.");
        }
        String time = submitted.getAppointmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        if (!availableSlots(doctor.getId(), submitted.getAppointmentDate().toLocalDate(), booking.getId()).contains(time))
            throw new IllegalArgumentException("That time is unavailable. Please choose another slot.");
        if (submitted.getContactEmail() == null || !submitted.getContactEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Enter a valid contact email.");
        if (submitted.getContactPhone() == null || !submitted.getContactPhone().matches("^\\+?[0-9\\s\\-()]{9,20}$"))
            throw new IllegalArgumentException("Enter a valid contact phone number.");
        booking.setDoctor(doctor);
        booking.setPatient(patient);
        booking.setAppointmentDate(submitted.getAppointmentDate());
        booking.setContactEmail(submitted.getContactEmail().trim());
        booking.setContactPhone(submitted.getContactPhone().trim());
        booking.setStatus("SCHEDULED");
        Appointment saved = appointments.save(booking);
        Notification confirmation = new Notification(saved.getContactEmail(), "Appointment with " + doctor.getName() + " confirmed for " + saved.getAppointmentDate(), Notification.NotificationType.CONFIRMATION, Notification.NotificationStatus.PENDING);
        confirmation.setAppointment(saved);
        notifications.save(confirmation);
        return saved;
    }
}
