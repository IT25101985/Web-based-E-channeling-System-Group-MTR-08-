package com.echanneling.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User patient;

    @OneToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    private String diagnosis;
    private LocalDateTime recordDate;
    private boolean isArchived = false;

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL)
    private java.util.List<Prescription> prescriptions = new java.util.ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String d) { this.diagnosis = d; }
    
    public LocalDateTime getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDateTime d) { this.recordDate = d; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public java.util.List<Prescription> getPrescriptions() { return prescriptions; }
    public void setPrescriptions(java.util.List<Prescription> p) { this.prescriptions = p; }
}
