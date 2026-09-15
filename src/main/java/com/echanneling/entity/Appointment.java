package com.echanneling.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;
    
    @ManyToOne
    private Doctor doctor;
    
    @ManyToOne
    private User patient;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime appointmentDate;
    private String contactPhone;
    private String contactEmail;
    private String status; // SCHEDULED, COMPLETED, CANCELLED
    
    // Additional fields for database reporting/display
    private String doctorName;
    private String patientName;
    private String date;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public LocalDateTime getAppointmentDate() { return appointmentDate; }
    
    /**
     * Encapsulation demo: Simple validation
     */
    public void setAppointmentDate(LocalDateTime date) { 
        this.appointmentDate = date; 
    }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @PrePersist
    @PreUpdate
    public void populateNames() {
        if (doctor != null) {
            this.doctorName = doctor.getName();
        }
        if (patient != null) {
            this.patientName = patient.getFullName();
        }
        if (appointmentDate != null) {
            this.date = appointmentDate.toLocalDate().toString();
        }
    }
}
