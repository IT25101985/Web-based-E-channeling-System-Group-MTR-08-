package com.echanneling.service;

import com.echanneling.entity.Invoice;
import com.echanneling.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired private com.echanneling.repository.InvoiceAuditRepository audit;
    @org.springframework.transaction.annotation.Transactional
    public void changeStatus(Long id,String status,String actor) {
        Invoice invoice=invoiceRepository.lock(id).orElseThrow();
        if(status.equals(invoice.getStatus())) return;
        if(!"PAID".equals(invoice.getStatus()) || !"REFUNDED".equals(status))
            throw new IllegalArgumentException("Use cash collection to record payment. Only paid invoices can be refunded.");
        audit.save(new com.echanneling.entity.InvoiceAudit(invoice,invoice.getStatus(),status,actor));
        invoice.setStatus(status);
    }

    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> forAppointments(List<com.echanneling.entity.Appointment> appointments) {
        return appointments.isEmpty() ? List.of() : invoiceRepository.findByAppointmentIn(appointments);
    }
    public java.math.BigDecimal paidRevenue() { return invoiceRepository.paidRevenue(); }
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    public void deleteInvoice(Long id) {
        throw new IllegalArgumentException("Invoices are retained for audit. Record a refund instead.");
    }

    public Optional<Invoice> getInvoiceByAppointment(com.echanneling.entity.Appointment app) {
        return invoiceRepository.findByAppointment(app);
    }
}
