package com.echanneling.controller;

import com.echanneling.entity.*;
import com.echanneling.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Optional;

/**
 * Controller to show how Polymorphism works in Payment.
 */
@Controller
public class PaymentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private UserService userService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PdfService pdfService;

    @Autowired private CheckoutService checkoutService;
    @GetMapping("/pay")
    public String paymentForm(@RequestParam Long appointmentId, Principal principal, Model model) {
        Appointment app = appointmentService.getAppointmentById(appointmentId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (app.getPatient() == null || !app.getPatient().getUsername().equals(principal.getName())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        model.addAttribute("appointment", app);
        return "patient/checkout";
    }
    @PostMapping("/pay")
    public String processPayment(@RequestParam String type, @RequestParam Long appointmentId, Principal principal, Model model) {
        model.addAttribute("invoice", checkoutService.checkout(appointmentId, principal.getName(), type));
        return "patient/payment-success";
    }

    // Only the owner of a completed appointment may submit its review.
    @PostMapping("/feedback/add")
    public String addFeedback(@RequestParam Long appointmentId, @RequestParam Integer rating,
                              @RequestParam(defaultValue = "") String comments, Principal principal,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            feedbackService.submit(appointmentId, principal.getName(), rating, comments);
            flash.addFlashAttribute("successMessage", "Your review was submitted for moderation.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/dashboard?section=history";
    }

    // Admin: View Invoices
    @GetMapping("/admin/invoices")
    public String viewAllInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "admin/invoice-management";
    }

    // Admin: Update Payment Status (e.g. Refund)
    @PostMapping("/admin/invoice/{id}/update")
    public String updateInvoiceStatus(@PathVariable Long id, @RequestParam String status, Principal principal) {
        if (!java.util.Set.of("PAID", "UNPAID", "REFUNDED").contains(status)) throw new IllegalArgumentException("Invalid invoice status");
        invoiceService.changeStatus(id,status,principal.getName());
        return "redirect:/admin/invoices";
    }

    // Admin: Delete/Refund Invoice
    @PostMapping("/admin/invoice/{id}/delete")
    public String deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return "redirect:/admin/invoices";
    }



    // Patient: Download Invoice/Summary
    @GetMapping("/invoice/download/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id, org.springframework.security.core.Authentication auth) {
        Optional<Invoice> optInv = invoiceService.getInvoiceById(id);
        if (optInv.isPresent()) {
            Invoice inv = optInv.get();
            boolean admin = auth.getAuthorities().stream().anyMatch(a -> java.util.Set.of("ROLE_ADMIN", "ROLE_RECEPTIONIST").contains(a.getAuthority()));
            if (!admin && (inv.getAppointment().getPatient() == null || !auth.getName().equals(inv.getAppointment().getPatient().getUsername()))) return org.springframework.http.ResponseEntity.status(403).build();
            try {
                java.io.ByteArrayInputStream pdfStream = pdfService.generateInvoice(inv);
                byte[] data = pdfStream.readAllBytes();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "invoice_" + id + ".pdf");
                return new org.springframework.http.ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.NOT_FOUND);
    }
}
