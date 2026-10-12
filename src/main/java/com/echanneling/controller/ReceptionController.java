package com.echanneling.controller;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.time.LocalDateTime;

/** Front-desk routes are restricted to receptionist/admin by SecurityConfig. */
@Controller
@RequestMapping("/reception")
public class ReceptionController {
    private final UserRepository users;
    private final HospitalBranchRepository branches;
    private final AppointmentRepository appointments;
    private final InvoiceRepository invoices;
    private final BookingService bookings;
    private final AppointmentService appointmentService;
    private final CheckoutService checkout;
    public ReceptionController(UserRepository users, HospitalBranchRepository branches,
            AppointmentRepository appointments, InvoiceRepository invoices, BookingService bookings,
            AppointmentService appointmentService, CheckoutService checkout) {
        this.users=users; this.branches=branches; this.appointments=appointments; this.invoices=invoices;
        this.bookings=bookings; this.appointmentService=appointmentService; this.checkout=checkout;
    }
    @GetMapping("/dashboard")
    public String dashboard(Model model, @RequestParam(required=false) Long reschedule, @RequestParam(defaultValue="0") int page) {
        Appointment selected = reschedule == null ? new Appointment() : appointments.findById(reschedule).orElseThrow();
        if (reschedule != null && !"SCHEDULED".equals(selected.getStatus()))
            throw new IllegalArgumentException("Only scheduled visits can be rescheduled.");
        model.addAttribute("appointment", selected);
        model.addAttribute("patients", users.findByRole("ROLE_PATIENT").stream().filter(User::isActive).toList());
        model.addAttribute("branches", branches.findAll());
        var history=appointmentService.allHistory(page);
        model.addAttribute("appointments",history.getContent());
        model.addAttribute("page",history.getNumber()); model.addAttribute("hasNext",history.hasNext());
        java.util.Map<Long, Invoice> invoiceMap = new java.util.HashMap<>();
        if(!history.isEmpty()) invoices.findByAppointmentIn(history.getContent()).forEach(i->invoiceMap.put(i.getAppointment().getId(),i));
        model.addAttribute("invoiceMap", invoiceMap);
        return "reception/dashboard";
    }
    @PostMapping("/book")
    public String book(@RequestParam Long patientId, @RequestParam Long doctorId,
                       @RequestParam Long branchId, @RequestParam LocalDateTime appointmentDate,
                       @RequestParam String contactEmail, @RequestParam String contactPhone,
                       @RequestParam(required=false) Long id, RedirectAttributes flash) {
        try {
            User patient = users.findById(patientId).orElseThrow(() -> new IllegalArgumentException("Choose a patient."));
            Appointment submitted = new Appointment(); submitted.setId(id);
            Doctor doctor = new Doctor(); doctor.setId(doctorId); submitted.setDoctor(doctor);
            HospitalBranch branch = new HospitalBranch(); branch.setId(branchId); submitted.setHospitalBranch(branch);
            submitted.setAppointmentDate(appointmentDate); submitted.setContactEmail(contactEmail); submitted.setContactPhone(contactPhone);
            bookings.book(patient, submitted);
            flash.addFlashAttribute("successMessage", id == null ? "Walk-in appointment booked." : "Appointment rescheduled.");
        } catch (IllegalArgumentException e) { flash.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/reception/dashboard";
    }
    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes flash) {
        try { appointmentService.cancel(id); flash.addFlashAttribute("successMessage", "Appointment cancelled. Notifications queued."); }
        catch (IllegalArgumentException e) { flash.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/reception/dashboard";
    }
    @PostMapping("/appointments/{id}/cash")
    public String collect(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try { checkout.collectCash(id,principal.getName()); flash.addFlashAttribute("successMessage", "Cash payment recorded. Receipt is available."); }
        catch (IllegalArgumentException e) { flash.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/reception/dashboard";
    }
}
