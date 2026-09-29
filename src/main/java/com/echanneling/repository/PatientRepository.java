package com.echanneling.repository;

import com.echanneling.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    // Inherits full secure CRUD architecture processing interfaces out-of-the-box
}
