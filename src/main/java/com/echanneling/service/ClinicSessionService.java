package com.echanneling.service;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** A dated clinic session connects a doctor to a hospital. Each visit occupies one hour. */
@Service
public class ClinicSessionService {
    private final AvailabilitySlotRepository slots;
    private final DoctorRepository doctors;
    private final HospitalBranchRepository branches;
    private final AppointmentRepository appointments;
    public ClinicSessionService(AvailabilitySlotRepository slots, DoctorRepository doctors,
            HospitalBranchRepository branches, AppointmentRepository appointments) {
        this.slots=slots; this.doctors=doctors; this.branches=branches; this.appointments=appointments;
    }
    public List<AvailabilitySlot> all() {
        return slots.findAll().stream().sorted(Comparator.comparing(AvailabilitySlot::getSlotDate)
                .thenComparing(AvailabilitySlot::getStartTime)).toList();
    }
    @Transactional
    public void save(Long id, Long doctorId, Long branchId, LocalDate date, LocalTime start, LocalTime end) {
        if (date == null || start == null || end == null || !date.atTime(start).isAfter(LocalDateTime.now())
                || !end.isAfter(start) || start.getSecond()!=0 || end.getSecond()!=0
                || Duration.between(start,end).toMinutes()%60!=0)
            throw new IllegalArgumentException("Choose a future session with a whole number of one-hour appointments.");
        Doctor doctor=doctors.findForBooking(doctorId).orElseThrow(()->new IllegalArgumentException("Choose a doctor."));
        HospitalBranch branch=branches.findById(branchId).orElseThrow(()->new IllegalArgumentException("Choose a hospital."));
        AvailabilitySlot session=id==null ? new AvailabilitySlot() : slots.findById(id).orElseThrow();
        if (id!=null && !session.getDoctor().getId().equals(doctorId))
            throw new IllegalArgumentException("Create a new session to change the doctor.");
        if (id!=null) ensureNoBookings(session);
        for (AvailabilitySlot other:slots.findByDoctorAndSlotDateAndIsActive(doctor,date,true)) {
            if (!Objects.equals(other.getId(),id) && start.isBefore(other.getEndTime()) && end.isAfter(other.getStartTime()))
                throw new IllegalArgumentException("This doctor already has an overlapping session, possibly at another hospital.");
        }
        // Legacy bookings without a branch still reserve the doctor's time.
        if (appointments.findByDoctorAndAppointmentDateBetween(doctor,date.atTime(start),date.atTime(end).minusNanos(1))
                .stream().anyMatch(a->!"CANCELLED".equals(a.getStatus())))
            throw new IllegalArgumentException("Existing appointments use this time. Choose another session.");
        session.setDoctor(doctor); session.setHospitalBranch(branch); session.setSlotDate(date);
        session.setStartTime(start); session.setEndTime(end); session.setIsActive(true);
        session.setMaxPatients((int)Duration.between(start,end).toHours()); session.setBookedCount(0);
        slots.save(session);
    }
    private void ensureNoBookings(AvailabilitySlot session) {
        if (appointments.findByDoctorAndAppointmentDateBetween(session.getDoctor(),session.getSlotDate().atTime(session.getStartTime()),
                session.getSlotDate().atTime(session.getEndTime()).minusNanos(1)).stream().anyMatch(a->!"CANCELLED".equals(a.getStatus())))
            throw new IllegalArgumentException("This session has appointments. Cancel or reschedule them before changing the session.");
    }
    @Transactional
    public void delete(Long id) {
        AvailabilitySlot session=slots.findById(id).orElseThrow();
        doctors.findForBooking(session.getDoctor().getId()).orElseThrow();
        ensureNoBookings(session); slots.delete(session);
    }
}
