package com.echanneling.repository;

import com.echanneling.entity.Appointment;
import com.echanneling.entity.Doctor;
import com.echanneling.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Appointment a where a.id = :id")
    java.util.Optional<Appointment> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Appointment> findByStatusAndAppointmentDateBetween(String status, java.time.LocalDateTime start, java.time.LocalDateTime end);
    boolean existsByHospitalBranchId(Long branchId);
    List<Appointment> findByPatient(User patient);
    List<Appointment> findByDoctor(Doctor doctor);
    List<Appointment> findByDoctorOrderByIdDesc(Doctor doctor);
    List<Appointment> findByDoctorAndAppointmentDateBetween(Doctor doctor, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
