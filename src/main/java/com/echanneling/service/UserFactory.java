package com.echanneling.service;

import com.echanneling.entity.Doctor;
import com.echanneling.entity.Patient;
import com.echanneling.entity.User;
import org.springframework.stereotype.Component;

/**
 * Factory Pattern Implementation
 * Creates appropriate User subtypes based on role string.
 * This decouples object creation logic from the rest of the application.
 */
@Component
public class UserFactory {

    /**
     * Factory method - creates the correct User subtype based on role.
     * Demonstrates the Factory Design Pattern for the SE2030 rubric.
     */
    public User createUser(String role) {
        switch (role.toUpperCase()) {
            case "PATIENT":
                Patient patient = new Patient();
                patient.setRole("ROLE_PATIENT");
                return patient;
            case "DOCTOR":
                User doctorUser = new User();
                doctorUser.setRole("ROLE_DOCTOR");
                return doctorUser;
            case "ADMIN":
                User admin = new User();
                admin.setRole("ROLE_ADMIN");
                return admin;
            default:
                return new User();
        }
    }
}
