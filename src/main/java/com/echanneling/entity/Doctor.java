package com.echanneling.entity;

import jakarta.persistence.*;

@Entity
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String specialization;
    private String experience;
    private String clinicHours;
    private Double consultationFee = 500.0; // Default fee
    
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { 
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (user != null && user.getFullName() != null) {
            return user.getFullName();
        }
        return name; 
    }
    public void setName(String name) { this.name = name; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public String getClinicHours() { return clinicHours; }
    public void setClinicHours(String clinicHours) { this.clinicHours = clinicHours; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(Double consultationFee) { this.consultationFee = consultationFee; }
}
