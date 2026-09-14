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
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
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
    
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }
    
    @org.springframework.transaction.annotation.Transactional
    public void deleteAppointment(Appointment appointment) {
        Long id = appointment.getId();
        jdbc.update("DELETE FROM notifications WHERE appointment_id = ?", id);
        jdbc.update("DELETE FROM prescription WHERE medical_record_id IN (SELECT id FROM medical_record WHERE appointment_id = ?)", id);
        jdbc.update("DELETE FROM medical_record WHERE appointment_id = ?", id);
        jdbc.update("DELETE FROM invoice WHERE appointment_id = ?", id);
        appointmentRepository.delete(appointment);
    }
}
