package com.echanneling.service;

import com.echanneling.entity.Feedback;
import com.echanneling.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {
    @Autowired private com.echanneling.repository.AppointmentRepository appointments;

    @org.springframework.transaction.annotation.Transactional
    public Feedback submit(Long appointmentId, String username, Integer rating, String comments) {
        if (rating == null || rating < 1 || rating > 5 || (comments != null && comments.length() > 1000))
            throw new IllegalArgumentException("Choose a rating from 1 to 5 and use at most 1000 characters.");
        var appointment = appointments.findLockedById(appointmentId).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (appointment.getPatient() == null || !username.equals(appointment.getPatient().getUsername()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        if (!"COMPLETED".equals(appointment.getStatus()))
            throw new IllegalArgumentException("Feedback is available after a completed visit.");
        // A row lock serializes duplicate submissions for this visit.
        if (feedbackRepository.existsByAppointmentId(appointmentId))
            throw new IllegalArgumentException("You have already reviewed this visit.");
        Feedback feedback = new Feedback();
        feedback.setAppointment(appointment);
        feedback.setPatient(appointment.getPatient());
        feedback.setDoctor(appointment.getDoctor());
        feedback.setRating(rating);
        feedback.setComments(comments == null ? "" : comments.trim());
        return feedbackRepository.save(feedback);
    }


    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback saveFeedback(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    public void deleteFeedback(Long id) {
        feedbackRepository.deleteById(id);
    }

    public List<Feedback> getFeedbackByDoctor(com.echanneling.entity.Doctor doctor) {
        return feedbackRepository.findByDoctor(doctor);
    }

    public List<Feedback> getFeedbackByPatient(com.echanneling.entity.User patient) {
        return feedbackRepository.findByPatient(patient);
    }

    /**
     * Admin Moderation - Approve a feedback review
     */
    public void approveFeedback(Long id) {
        Optional<Feedback> opt = feedbackRepository.findById(id);
        opt.ifPresent(f -> {
            f.setModerationStatus("APPROVED");
            feedbackRepository.save(f);
        });
    }

    /**
     * Admin Moderation - Reject a feedback review
     */
    public void rejectFeedback(Long id) {
        Optional<Feedback> opt = feedbackRepository.findById(id);
        opt.ifPresent(f -> {
            f.setModerationStatus("REJECTED");
            feedbackRepository.save(f);
        });
    }

    /**
     * Get only approved feedbacks (for public display)
     */
    public List<Feedback> getApprovedFeedback() {
        return feedbackRepository.findByModerationStatus("APPROVED");
    }
}
