package com.echanneling.service;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

/** Strategy context: delegates payment behavior to the selected, injected strategy. */
@Service
public class CheckoutService {
    @org.springframework.beans.factory.annotation.Value("${app.payments.demo-card-enabled:false}") private boolean demoCards;
    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final InvoiceRepository invoices;
    private final Map<String, Payment> strategies;
    public CheckoutService(AppointmentRepository a, DoctorRepository d, InvoiceRepository i,
                           CardPayment card, CashPayment cash) {
        appointments = a; doctors = d; invoices = i;
        strategies = Map.of("card", card, "cash", cash);
    }
    @Transactional
    public Invoice checkout(Long id, String username, String type) {
        if ("card".equals(type) && !demoCards) throw new IllegalArgumentException("Card payments are unavailable. Choose counter cash.");
        Payment payment = strategies.get(type);
        if (payment == null) throw new IllegalArgumentException("Choose card or cash.");
        Appointment initial = appointments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Consistent doctor -> appointment lock order with booking and rescheduling.
        doctors.findForBooking(initial.getDoctor().getId()).orElseThrow();
        Appointment a = appointments.findLockedById(id).orElseThrow();
        if (a.getPatient() == null || !username.equals(a.getPatient().getUsername()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if ("CANCELLED".equals(a.getStatus())) throw new IllegalArgumentException("Cancelled appointments cannot be paid.");
        var existing = invoices.findByAppointment(a);
        if (existing.isPresent()) return existing.get();
        Invoice invoice = new Invoice();
        invoice.setAppointment(a);
        if (a.getBookedConsultationFee() == null) a.setBookedConsultationFee(a.getDoctor().getConsultationFee());
        invoice.setAmount(a.getBookedConsultationFee());
        payment.processPayment(invoice.getAmount());
        invoice.setPaymentMethod(type.toUpperCase(java.util.Locale.ROOT));
        invoice.setStatus("card".equals(type) ? "PAID" : "UNPAID");
        return invoices.save(invoice);
    }
    @Transactional
    public Invoice collectCash(Long appointmentId, String staffUsername) {
        Appointment appointment = appointments.findById(appointmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Invoice invoice = checkout(appointmentId, appointment.getPatient().getUsername(), "cash");
        if ("PAID".equals(invoice.getStatus()) && "CASH".equals(invoice.getPaymentMethod())) return invoice;
        if (!"UNPAID".equals(invoice.getStatus()) || (invoice.getPaymentMethod() != null && !"CASH".equals(invoice.getPaymentMethod())))
            throw new IllegalArgumentException("Only unpaid cash invoices can be collected.");
        invoice.setPaymentMethod("CASH");
        invoice.setStatus("PAID");
        invoice.setCollectedBy(staffUsername);
        invoice.setCollectedAt(java.time.LocalDateTime.now());
        return invoices.save(invoice);
    }
}
