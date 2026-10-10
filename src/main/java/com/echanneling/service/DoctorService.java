package com.echanneling.service;

import com.echanneling.entity.Doctor;
import com.echanneling.entity.Specialization;
import com.echanneling.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DoctorService {
    @Autowired
    private DoctorRepository doctorRepository;

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }
    
    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }
    
    public Doctor saveDoctor(Doctor doctor) {
        if (doctor.getConsultationFee() == null || !Double.isFinite(doctor.getConsultationFee()) || doctor.getConsultationFee() <= 0) throw new IllegalArgumentException("Consultation fee must be positive");
        if (doctor.getName() == null || doctor.getName().isBlank() || doctor.getSpecialization() == null || doctor.getSpecialization().isBlank()) throw new IllegalArgumentException("Doctor name and specialization are required");
        if (doctor.getClinicHours() == null || !doctor.getClinicHours().matches("(?i)^(0[1-9]|1[0-2]):[0-5][0-9] (AM|PM) - (0[1-9]|1[0-2]):[0-5][0-9] (AM|PM)$")) throw new IllegalArgumentException("Use clinic hours such as 09:00 AM - 05:00 PM");
        String[] hours = doctor.getClinicHours().toUpperCase(java.util.Locale.ROOT).split(" - ");
        var format = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);
        if (!java.time.LocalTime.parse(hours[0], format).isBefore(java.time.LocalTime.parse(hours[1], format))) throw new IllegalArgumentException("Clinic end time must be after its start time");
        return doctorRepository.save(doctor);
    }

    public void deleteDoctor(Long id) {
        doctorRepository.deleteById(id);
    }

    // Polymorphism search
    public List<Doctor> searchBySpeciality(Specialization spec) {
        return doctorRepository.findBySpecialization(spec.getSpecialtyName());
    }

    public Optional<Doctor> getDoctorByUser(com.echanneling.entity.User user) {
        return doctorRepository.findByUser(user);
    }
}
