package com.echanneling.repository;

import com.echanneling.entity.Appointment;
import com.echanneling.entity.Doctor;
import com.echanneling.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatient(User patient);
    List<Appointment> findByDoctor(Doctor doctor);
    List<Appointment> findByDoctorOrderByIdDesc(Doctor doctor);
    List<Appointment> findByDoctorAndAppointmentDateBetween(Doctor doctor, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
