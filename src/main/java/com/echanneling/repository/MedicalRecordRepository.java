package com.echanneling.repository;

import com.echanneling.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.echanneling.entity.User;
import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatient(User patient);
    List<MedicalRecord> findByAppointmentIn(java.util.Collection<com.echanneling.entity.Appointment> appointments);
    @org.springframework.data.jpa.repository.Query("select r from MedicalRecord r where r.appointment.doctor.user.username = :username and r.isArchived = false order by r.recordDate desc")
    org.springframework.data.domain.Page<MedicalRecord> findByAppointmentDoctorUserUsernameAndArchivedFalseOrderByRecordDateDesc(String username, org.springframework.data.domain.Pageable page);
    @org.springframework.data.jpa.repository.Query("select r from MedicalRecord r where r.patient = :patient and (r.appointment is null or r.appointment.bookingFor is null or r.appointment.bookingFor = 'self') order by r.recordDate desc")
    List<MedicalRecord> latestPersonal(@org.springframework.data.repository.query.Param("patient") User patient, org.springframework.data.domain.Pageable page);
    List<MedicalRecord> findByPatientOrderByRecordDateDesc(User patient);
    java.util.Optional<MedicalRecord> findByAppointment(com.echanneling.entity.Appointment appointment);
}
