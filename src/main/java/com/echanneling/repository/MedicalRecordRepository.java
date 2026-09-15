package com.echanneling.repository;

import com.echanneling.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.echanneling.entity.User;
import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatient(User patient);
    List<MedicalRecord> findByPatientOrderByRecordDateDesc(User patient);
    java.util.Optional<MedicalRecord> findByAppointment(com.echanneling.entity.Appointment appointment);
}
