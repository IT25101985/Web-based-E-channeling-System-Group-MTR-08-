package com.echanneling.entity;

import jakarta.persistence.*;

@Entity
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private MedicalRecord medicalRecord;

    private String medication;
    private String dosage;
    private String duration;
    private Double price = 0.0;

    // Encapsulation
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MedicalRecord getMedicalRecord() { return medicalRecord; }
    public void setMedicalRecord(MedicalRecord medicalRecord) { this.medicalRecord = medicalRecord; }
    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
