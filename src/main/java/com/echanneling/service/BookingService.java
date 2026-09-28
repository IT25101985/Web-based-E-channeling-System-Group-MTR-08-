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
    private final NotificationService notifications;
    private final AvailabilitySlotRepository sessions;
    private final HospitalBranchRepository branches;
    public BookingService(DoctorRepository doctors, AppointmentRepository appointments, NotificationService notifications, AvailabilitySlotRepository sessions, HospitalBranchRepository branches) {
        this.doctors = doctors;
        this.appointments = appointments;
        this.notifications = notifications;
        this.sessions = sessions; this.branches = branches;
    }

    public List<String> availableSlots(Long doctorId, Long branchId, LocalDate date, Long excludeId) {
        Doctor doctor = doctors.findById(doctorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (branchId == null || date.isBefore(LocalDate.now())) return List.of();
        List<Appointment> booked = appointments.findByDoctorAndAppointmentDateBetween(doctor, date.atStartOfDay(), date.atTime(LocalTime.MAX));
        Set<String> result = new TreeSet<>();
        for (AvailabilitySlot session : sessions.findByDoctorAndSlotDateAndIsActive(doctor, date, true)) {
            if (!session.getHospitalBranch().getId().equals(branchId)) continue;
            for (LocalDateTime time=date.atTime(session.getStartTime()); !time.plusHours(1).isAfter(date.atTime(session.getEndTime())); time=time.plusHours(1)) {
                LocalDateTime slot=time;
                if (slot.isAfter(LocalDateTime.now()) && booked.stream().noneMatch(a ->
                        !Objects.equals(a.getId(), excludeId) && !"CANCELLED".equals(a.getStatus())
                                && a.getAppointmentDate().isBefore(slot.plusHours(1)) && a.getAppointmentDate().plusHours(1).isAfter(slot)))
                    result.add(slot.toString());
            }
        }
        return new ArrayList<>(result);
    }

    @Transactional
    public Appointment book(User patient, Appointment submitted) {
        if (submitted.getDoctor() == null || submitted.getDoctor().getId() == null || submitted.getAppointmentDate() == null || submitted.getHospitalBranch() == null || submitted.getHospitalBranch().getId() == null)
            throw new IllegalArgumentException("Choose a hospital, doctor, date and available time.");
        // Lock the doctor's database row until commit, including concurrent requests on other app instances.
        Doctor doctor = doctors.findForBooking(submitted.getDoctor().getId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor is no longer available."));
        Appointment booking = new Appointment();
        if (submitted.getId() != null) {
            booking = appointments.findLockedById(submitted.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (booking.getPatient() == null || !Objects.equals(booking.getPatient().getId(), patient.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            if (!"SCHEDULED".equals(booking.getStatus())) throw new IllegalArgumentException("Only scheduled appointments can be rescheduled.");
            if (!Objects.equals(booking.getDoctor().getId(), doctor.getId()))
                throw new IllegalArgumentException("Keep the same doctor when rescheduling. Cancel and make a new booking to change doctors.");
        }
        HospitalBranch branch=branches.findById(submitted.getHospitalBranch().getId()).orElseThrow(()->new IllegalArgumentException("Hospital is no longer available."));
        if (submitted.getAppointmentDate().getSecond()!=0 || submitted.getAppointmentDate().getNano()!=0)
            throw new IllegalArgumentException("Choose an exact appointment time.");
        String time = submitted.getAppointmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        if (!availableSlots(doctor.getId(), branch.getId(), submitted.getAppointmentDate().toLocalDate(), booking.getId()).contains(time))
            throw new IllegalArgumentException("That time is unavailable. Please choose another slot.");
        if (submitted.getContactEmail() == null || !submitted.getContactEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Enter a valid contact email.");
        if (submitted.getContactPhone() == null || !submitted.getContactPhone().matches("^\\+?[0-9\\s\\-()]{9,20}$"))
            throw new IllegalArgumentException("Enter a valid contact phone number.");
        booking.setDoctor(doctor);
        booking.setHospitalBranch(branch);
        booking.setPatient(patient);
        booking.setAppointmentDate(submitted.getAppointmentDate());
        booking.setContactEmail(submitted.getContactEmail().trim());
        booking.setContactPhone(submitted.getContactPhone().trim());
        booking.setStatus("SCHEDULED");
        Appointment saved = appointments.save(booking);
        notifications.appointmentEvent(saved, submitted.getId()==null ? Notification.NotificationType.CONFIRMATION : Notification.NotificationType.RESCHEDULE);
        return saved;
    }
}
