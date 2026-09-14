package com.echanneling.repository;

import com.echanneling.entity.AvailabilitySlot;
import com.echanneling.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    List<AvailabilitySlot> findByDoctor(Doctor doctor);
    List<AvailabilitySlot> findByDoctorAndSlotDateAndIsActive(Doctor doctor, LocalDate date, Boolean isActive);
    List<AvailabilitySlot> findBySlotDateAndIsActive(LocalDate date, Boolean isActive);
}
