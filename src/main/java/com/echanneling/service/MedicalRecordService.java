package com.echanneling.service;

import com.echanneling.entity.MedicalRecord;
import com.echanneling.entity.Prescription;
import com.echanneling.entity.User;
import com.echanneling.repository.MedicalRecordRepository;
import com.echanneling.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    public MedicalRecord saveMedicalRecord(MedicalRecord record) {
        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDateTime.now());
        }
        return medicalRecordRepository.save(record);
    }

    public Prescription savePrescription(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    public List<MedicalRecord> getRecordsByPatient(User patient) {
        return medicalRecordRepository.findByPatient(patient);
    }

    public List<MedicalRecord> getRecentRecordsByPatient(User patient) {
        return medicalRecordRepository.findByPatientOrderByRecordDateDesc(patient);
    }

    public Optional<MedicalRecord> getRecordById(Long id) {
        return medicalRecordRepository.findById(id);
    }

    public void deleteRecord(Long id) {
        medicalRecordRepository.deleteById(id);
    }

    public Optional<MedicalRecord> getRecordByAppointment(com.echanneling.entity.Appointment app) {
        return medicalRecordRepository.findByAppointment(app);
    }
}
