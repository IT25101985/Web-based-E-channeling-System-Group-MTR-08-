package com.echanneling.controller;

import com.echanneling.entity.*;
import com.echanneling.service.AppointmentService;
import com.echanneling.service.MedicalRecordService;
import com.echanneling.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/doctor")
public class DoctorScheduleController {
    private boolean owns(Appointment appointment, Principal principal) {
        return principal != null && appointment != null && appointment.getDoctor() != null && appointment.getDoctor().getUser() != null && principal.getName().equals(appointment.getDoctor().getUser().getUsername());
    }
    private void requireOwner(Appointment appointment, Principal principal) {
        if (!owns(appointment, principal)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
    }


    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private UserService userService;

    // 1. View Appointments (Doctor-oda schedule-ai mattum filter panni kaatta)
    @GetMapping("/appointments")
    public String viewAppointments(Model model, Principal principal, @RequestParam(defaultValue="0") int page) {
        if (principal != null) {
            String username = principal.getName();

            // Logged-in doctor-oda username-ku match aagura appointments mattum edukkirom
            var history = appointmentService.doctorHistory(username,page);
            List<Appointment> doctorAppointments = history.getContent();

            model.addAttribute("appointments", doctorAppointments);
            model.addAttribute("page", Math.max(0,page));
            model.addAttribute("hasNext", history.hasNext());
        } else {
            model.addAttribute("appointments", new ArrayList<>());
        }
        return "doctor/view-appointments";
    }

    // 2. Patient List (System-la irukkura patients list-ai kaatta)
    @GetMapping("/patients")
    public String viewPatients(Model model, Principal principal, @RequestParam(defaultValue="0") int page) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
            var history = appointmentService.doctorHistory(principal.getName(), page);
            List<Appointment> patients = history.getContent();
            model.addAttribute("page",history.getNumber()); model.addAttribute("hasNext",history.hasNext());
            model.addAttribute("patients", patients);
        }
        return "doctor/patient-list";
    }

    // 3. Patient Records (Historical Data)
    @GetMapping("/records")
    public String patientRecords(Model model, Principal principal, @RequestParam(defaultValue="0") int page) {
        if (principal != null) {
            String username = principal.getName();
            var history = medicalRecordService.recordsForDoctor(username, page);
            List<MedicalRecord> allRecords = history.getContent();
            model.addAttribute("page",history.getNumber()); model.addAttribute("hasNext",history.hasNext());
            model.addAttribute("records", allRecords);
            model.addAttribute("username", username);
        }
        return "doctor/records";
    }

    @Autowired
    private com.echanneling.service.DoctorService doctorService;

    @Autowired
    private com.echanneling.repository.PatientRepository patientRepository;

    // 3. Consultations (Doctor-oda schedule timing-la nadakkura consultations)
    @GetMapping("/consultations")
    public String consultations(@RequestParam(value = "appointmentId", required = false) Long appointmentId, Model model, Principal principal, @RequestParam(defaultValue="0") int page) {
        if (principal != null) {
            String username = principal.getName();
            model.addAttribute("username", username);

            Optional<User> optUser = userService.findByUsername(username);
            if (optUser.isPresent()) {
                Optional<Doctor> optDoc = doctorService.getDoctorByUser(optUser.get());
                if (optDoc.isPresent()) {
                    Doctor doc = optDoc.get();
                    Appointment activeAppt = null;
                    if (appointmentId != null) {
                        activeAppt = appointmentService.getAppointmentById(appointmentId).orElse(null);
                        requireOwner(activeAppt, principal);
                    }
                    if (activeAppt == null) activeAppt = appointmentService.nextConsultation(doc).orElse(null);
                    model.addAttribute("appointment", activeAppt);
                    if (activeAppt != null && !activeAppt.isForSomeoneElse() && activeAppt.getPatient() != null) {
                        User patientUser = activeAppt.getPatient();
                        Patient patientDetails = patientRepository.findById(patientUser.getId()).orElse(null);
                        model.addAttribute("patient", patientDetails);
                    }
                }
            }
        }
        return "doctor/consultations";
    }

    // 4. Prescribe / Write Medical Record
    @GetMapping("/prescribe/{appointmentId}")
    public String prescribeForm(@PathVariable Long appointmentId, Model model, Principal principal) {
        Optional<Appointment> optApp = appointmentService.getAppointmentById(appointmentId);
        if (optApp.isPresent()) {
            requireOwner(optApp.get(), principal);
            model.addAttribute("appointment", optApp.get());
            model.addAttribute("medicalRecord", new MedicalRecord());
            model.addAttribute("prescription", new Prescription());
            return "doctor/write-prescription";
        }
        return "redirect:/doctor/appointments";
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/prescribe/{appointmentId}")
    public String savePrescription(@PathVariable Long appointmentId,
                                   @RequestParam String diagnosis,
                                   @RequestParam String medication,
                                   @RequestParam String dosage,
                                   @RequestParam String duration,
                                   @RequestParam java.math.BigDecimal price, Principal principal) {
        if (diagnosis.isBlank() || medication.isBlank() || dosage.isBlank() || duration.isBlank() || price == null || price.signum() < 0) throw new IllegalArgumentException("Complete the prescription fields");
        Optional<Appointment> optApp = appointmentService.getLockedAppointment(appointmentId);
        if (optApp.isPresent()) {
            requireOwner(optApp.get(), principal);
            Appointment app = optApp.get();
            if (!"CHECKED_IN".equals(app.getStatus()) || app.getAppointmentDate() == null || app.getAppointmentDate().toLocalDate().isAfter(com.echanneling.patterns.ClinicClock.getInstance().today()) || medicalRecordService.getRecordByAppointment(app).isPresent()) return "redirect:/doctor/appointments?error=already_processed";

            // Create and save medical record
            MedicalRecord record = new MedicalRecord();
            record.setPatient(app.getPatient());
            record.setAppointment(app); // Linking record to appointment
            record.setDiagnosis(diagnosis);
            record.setRecordDate(LocalDateTime.now());
            medicalRecordService.saveMedicalRecord(record);

            // Create and save prescription
            Prescription prescription = new Prescription();
            prescription.setMedicalRecord(record);
            prescription.setMedication(medication);
            prescription.setDosage(dosage);
            prescription.setDuration(duration);
            prescription.setPrice(price); // Set medicine price
            medicalRecordService.savePrescription(prescription);

            // Mark appointment as COMPLETED
            app.setStatus("COMPLETED");
            appointmentService.saveAppointment(app);
        }
        return "redirect:/doctor/appointments?success";
    }

    @GetMapping("/record/{id}/edit")
    public String editRecordForm(@PathVariable Long id, Model model, Principal principal) {
        Optional<MedicalRecord> optRecord = medicalRecordService.getRecordById(id);
        if (optRecord.isPresent()) {
            requireOwner(optRecord.get().getAppointment(), principal);
            model.addAttribute("medicalRecord", optRecord.get());
            return "doctor/edit-record";
        }
        return "redirect:/doctor/records";
    }

    @PostMapping("/record/{id}/update")
    public String updateRecord(@PathVariable Long id, @RequestParam String diagnosis, Principal principal) {
        if (diagnosis.isBlank()) throw new IllegalArgumentException("Diagnosis is required");
        Optional<MedicalRecord> optRecord = medicalRecordService.getRecordById(id);
        if (optRecord.isPresent()) {
            requireOwner(optRecord.get().getAppointment(), principal);
            MedicalRecord record = optRecord.get();
            record.setDiagnosis(diagnosis);
            medicalRecordService.saveMedicalRecord(record);
        }
        return "redirect:/doctor/records?updated";
    }

    @PostMapping("/record/{id}/archive")
    public String archiveRecord(@PathVariable Long id, Principal principal) {
        Optional<MedicalRecord> optRecord = medicalRecordService.getRecordById(id);
        if (optRecord.isPresent()) {
            requireOwner(optRecord.get().getAppointment(), principal);
            MedicalRecord record = optRecord.get();
            record.setArchived(true);
            medicalRecordService.saveMedicalRecord(record);
        }
        return "redirect:/doctor/records?archived";
    }
}
