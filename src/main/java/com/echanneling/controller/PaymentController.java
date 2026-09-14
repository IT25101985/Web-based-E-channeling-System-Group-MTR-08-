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

    @GetMapping("/pay")
    public String processPayment(@RequestParam("type") String type, 
                                 @RequestParam("amount") Double amount,
                                 @RequestParam("appointmentId") Long appointmentId,
                                 Model model) {
        // Polymorphism demo
        Payment paymentMethod = type.equalsIgnoreCase("card") ? new CardPayment() : new CashPayment();
        paymentMethod.processPayment(amount);

        // Save Invoice to DB
        Optional<Appointment> optApp = appointmentService.getAppointmentById(appointmentId);
        if (optApp.isPresent()) {
            Invoice invoice = new Invoice();
            invoice.setAppointment(optApp.get());
            invoice.setAmount(amount);
            invoice.setStatus("PAID");
            invoiceService.saveInvoice(invoice);
            
            model.addAttribute("invoice", invoice);
            return "patient/payment-success";
        }
        return "redirect:/patient/history";
    }

    // Feedback creation
    @PostMapping("/feedback/add")
    public String addFeedback(@RequestParam Long doctorId, 
                              @RequestParam Integer rating, 
                              @RequestParam String comments, 
                              Principal principal) {
        if (principal != null) {
            Optional<User> optPatient = userService.findByUsername(principal.getName());
            Optional<Doctor> optDoctor = doctorService.getDoctorById(doctorId);

            if (optPatient.isPresent() && optDoctor.isPresent()) {
                Feedback feedback = new Feedback();
                feedback.setPatient(optPatient.get());
                feedback.setDoctor(optDoctor.get());
                feedback.setRating(rating);
                feedback.setComments(comments);
                feedbackService.saveFeedback(feedback);
            }
        }
        return "redirect:/patient/history?feedback_success";
    }

    // Admin: View Invoices
    @GetMapping("/admin/invoices")
    public String viewAllInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "admin/invoice-management";
    }

    // Admin: Update Payment Status (e.g. Refund)
    @PostMapping("/admin/invoice/{id}/update")
    public String updateInvoiceStatus(@PathVariable Long id, @RequestParam String status) {
        Optional<Invoice> optInvoice = invoiceService.getInvoiceById(id);
        if (optInvoice.isPresent()) {
            Invoice invoice = optInvoice.get();
            invoice.setStatus(status);
            invoiceService.saveInvoice(invoice);
        }
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
    public org.springframework.http.ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        Optional<Invoice> optInv = invoiceService.getInvoiceById(id);
        if (optInv.isPresent()) {
            Invoice inv = optInv.get();
            String content = "--- HEALTHCAREPLUS MEDICAL INVOICE ---\n\n" +
                             "Invoice ID: #" + inv.getId() + "\n" +
                             "Patient: " + inv.getAppointment().getPatient().getFullName() + "\n" +
                             "Doctor: " + inv.getAppointment().getDoctor().getName() + "\n" +
                             "Date: " + inv.getAppointment().getAppointmentDate() + "\n" +
                             "Amount Paid: $" + inv.getAmount() + "\n" +
                             "Status: " + inv.getStatus() + "\n\n" +
                             "Thank you for choosing HealthcarePlus!";
            
            byte[] data = content.getBytes();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "invoice_" + id + ".txt");
            return new org.springframework.http.ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
        }
        return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.NOT_FOUND);
    }
}
