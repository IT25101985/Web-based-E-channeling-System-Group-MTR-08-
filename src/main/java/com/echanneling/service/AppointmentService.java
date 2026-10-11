package com.echanneling.service;

import com.echanneling.entity.Appointment;
import com.echanneling.entity.Doctor;
import com.echanneling.entity.User;
import com.echanneling.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {
    @org.springframework.transaction.annotation.Transactional
    public void checkIn(Long id) {
        Appointment appointment = appointmentRepository.findLockedById(id).orElseThrow();
        if ("CHECKED_IN".equals(appointment.getStatus())) return;
        if (!"SCHEDULED".equals(appointment.getStatus())) throw new IllegalArgumentException("Only scheduled appointments can be checked in.");
        if (appointment.getAppointmentDate() == null || !appointment.getAppointmentDate().toLocalDate().equals(com.echanneling.patterns.ClinicClock.getInstance().today()))
            throw new IllegalArgumentException("Check-in is available only on the appointment date.");
        appointment.setStatus("CHECKED_IN");
    }

    public java.util.Optional<Appointment> getLockedAppointment(Long id) {
        return appointmentRepository.findLockedById(id);
    }

    @Autowired private NotificationService notifications;
    @org.springframework.transaction.annotation.Transactional
    public void cancel(Long id) {
        Appointment a=appointmentRepository.findLockedById(id).orElseThrow();
        if (!"SCHEDULED".equals(a.getStatus())) throw new IllegalArgumentException("Only scheduled appointments can be cancelled.");
        a.setStatus("CANCELLED");
        notifications.appointmentEvent(a,com.echanneling.entity.Notification.NotificationType.CANCELLATION);
    }

    @Autowired
    private AppointmentRepository appointmentRepository;

    public Appointment saveAppointment(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }

    public java.util.Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    public List<Appointment> getAppointmentsForPatient(User patient) {
        return appointmentRepository.findByPatient(patient);
    }

    public List<Appointment> getAppointmentsForDoctor(Doctor doctor) {
        return appointmentRepository.findByDoctor(doctor);
    }

    public List<Appointment> getRecentAppointmentsForDoctor(Doctor doctor) {
        return appointmentRepository.findByDoctorOrderByIdDesc(doctor);
    }

    public org.springframework.data.domain.Page<Appointment> patientHistory(User patient, int page) {
        return appointmentRepository.findByPatientAndHiddenByPatientFalseOrderByAppointmentDateDesc(patient,
            org.springframework.data.domain.PageRequest.of(Math.max(0, page), 20));
    }
    public org.springframework.data.domain.Page<Appointment> doctorHistory(String username, int page) {
        return appointmentRepository.findByDoctorUserUsernameOrderByAppointmentDateDesc(username,
            org.springframework.data.domain.PageRequest.of(Math.max(0, page), 20));
    }
    public java.util.Optional<Appointment> nextConsultation(Doctor doctor) { return appointmentRepository.findFirstByDoctorAndStatusOrderByAppointmentDateAsc(doctor,"CHECKED_IN"); }
    public long count() { return appointmentRepository.count(); }
    public long statusCount(String status) { return appointmentRepository.countByStatus(status); }
    public long todayCount() { var today=com.echanneling.patterns.ClinicClock.getInstance().today();return appointmentRepository.countByAppointmentDateBetween(today.atStartOfDay(),today.atTime(java.time.LocalTime.MAX)); }
    public java.util.Map<Integer,Long> hourlyCounts() {
        java.util.Map<Integer,Long> result=new java.util.HashMap<>();
        appointmentRepository.hourlyCounts().forEach(row -> result.put(((Number)row[0]).intValue(),((Number)row[1]).longValue()));return result;
    }
    public List<Appointment> recent(int size) { return appointmentRepository.findAll(org.springframework.data.domain.PageRequest.of(0,size,org.springframework.data.domain.Sort.by("id").descending())).getContent(); }
    public org.springframework.data.domain.Page<Appointment> allHistory(int page) {
        return appointmentRepository.findAll(org.springframework.data.domain.PageRequest.of(Math.max(0,page),20,org.springframework.data.domain.Sort.by("appointmentDate").descending()));
    }
    public long doctorToday(Doctor doctor) {
        var today=com.echanneling.patterns.ClinicClock.getInstance().today();return appointmentRepository.countByDoctorAndAppointmentDateBetween(doctor,today.atStartOfDay(),today.atTime(java.time.LocalTime.MAX));
    }
    public long doctorPending(Doctor doctor) {
        return appointmentRepository.countByDoctorAndStatus(doctor,"SCHEDULED")+appointmentRepository.countByDoctorAndStatus(doctor,"CHECKED_IN");
    }
    public long patientStatusCount(User patient, String status) { return appointmentRepository.countByPatientAndStatus(patient, status); }
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteAppointment(Appointment appointment) {
        Appointment stored = appointmentRepository.findLockedById(appointment.getId()).orElseThrow();
        if (!"CANCELLED".equals(stored.getStatus())) throw new IllegalArgumentException("Cancel the appointment before hiding it.");
        stored.setHiddenByPatient(true);
    }
}
