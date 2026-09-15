package com.echanneling.controller;

import com.echanneling.entity.*;
import com.echanneling.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/patient")
public class PatientBookingController {

    @Autowired
    private UserService userService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private InvoiceService invoiceService;

    // Dashboard mapping has been moved to PatientProfileController for workload distribution and advanced analytics integration.

    @GetMapping("/book-appointment")
    public String bookAppointmentForm() {
        return "redirect:/patient/dashboard?section=booking";
    }

    @Autowired private BookingService bookingService;

    @InitBinder("appointment")
    public void bookingFields(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("id", "doctor.id", "appointmentDate", "contactEmail", "contactPhone");
    }

    @PostMapping("/book-appointment")
    public String saveAppointment(@ModelAttribute("appointment") Appointment appointment,
                                  org.springframework.validation.BindingResult binding,
                                  Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        if (principal == null) return "redirect:/login";
        if (binding.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Please check your appointment details.");
            return "redirect:/patient/dashboard?section=booking";
        }
        try {
            bookingService.book(userService.findByUsername(principal.getName()).orElseThrow(), appointment);
            flash.addFlashAttribute("successMessage", "Your appointment is confirmed.");
            return "redirect:/patient/dashboard?section=history";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/patient/dashboard?section=booking";
        }
    }

    @GetMapping("/history")
    public String viewHistory() {
        return "redirect:/patient/dashboard?section=history";
    }

    @PostMapping("/appointment/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, Principal principal) {
        if (principal != null) {
            Optional<Appointment> optApp = appointmentService.getAppointmentById(id);
            if (optApp.isPresent() && optApp.get().getPatient().getUsername().equals(principal.getName())) {
                Appointment app = optApp.get();
                if (!"SCHEDULED".equals(app.getStatus())) return "redirect:/patient/dashboard?section=history&error=invalid_status";
                app.setStatus("CANCELLED");
                appointmentService.saveAppointment(app);
            }
        }
        return "redirect:/patient/dashboard?section=history&cancelled";
    }

    @PostMapping("/appointment/{id}/delete")
    public String deleteAppointment(@PathVariable Long id, Principal principal) {
        if (principal != null) {
            Optional<Appointment> optApp = appointmentService.getAppointmentById(id);
            if (optApp.isPresent() && optApp.get().getPatient().getUsername().equals(principal.getName())) {
                if (!"CANCELLED".equals(optApp.get().getStatus())) return "redirect:/patient/dashboard?section=history&error=cancel_first";
                appointmentService.deleteAppointment(optApp.get());
            }
        }
        return "redirect:/patient/dashboard?section=history&deleted";
    }

    @GetMapping("/appointment/{id}/reschedule")
    public String rescheduleForm(@PathVariable Long id) {
        return "redirect:/patient/dashboard?section=booking&reschedule=" + id;
    }

    @GetMapping("/appointment/{id}/record")
    public String viewPrescription(@PathVariable Long id, Model model, Principal principal) {
        if (principal != null) {
            Optional<Appointment> optApp = appointmentService.getAppointmentById(id);
            if (optApp.isPresent() && optApp.get().getPatient().getUsername().equals(principal.getName())) {
                Optional<MedicalRecord> optRecord = medicalRecordService.getRecordByAppointment(optApp.get());
                if (optRecord.isPresent()) {
                    model.addAttribute("record", optRecord.get());
                    return "patient/view-record";
                }
            }
        }
        return "redirect:/patient/dashboard?section=history&error=no_record";
    }
}
