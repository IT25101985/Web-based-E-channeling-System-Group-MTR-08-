package com.echanneling.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PATIENT")
public class Patient extends User {
    @jakarta.validation.constraints.NotBlank(message = "NIC is required")
    @jakarta.validation.constraints.Pattern(regexp = "^([0-9]{9}[xXvV]|[0-9]{12})$", message = "Invalid Sri Lankan NIC format")
    private String nic;

    private String bloodGroup;
    private String bloodPressure;
    private String heartRate;
    private String emergencyContact;

    // Default constructor implementing structural super references
    public Patient() {
        super();
        this.setRole("ROLE_PATIENT");
    }

    // Extended Constructor explicitly demonstrating structural OOP Inheritance execution 
    public Patient(String username, String password, String fullName, String bloodGroup) {
        super(username, password, fullName);
        this.bloodGroup = bloodGroup;
        this.setRole("ROLE_PATIENT");
    }

    // Safe Encapsulation Setters & Getters
    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public String getHeartRate() { return heartRate; }
    public void setHeartRate(String heartRate) { this.heartRate = heartRate; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
}
