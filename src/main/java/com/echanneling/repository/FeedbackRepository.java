package com.echanneling.repository;

import com.echanneling.entity.Doctor;
import com.echanneling.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsByAppointmentId(Long appointmentId);
    List<Feedback> findByDoctor(Doctor doctor);
    List<Feedback> findByPatient(com.echanneling.entity.User patient);
    List<Feedback> findByModerationStatus(String moderationStatus);
}
