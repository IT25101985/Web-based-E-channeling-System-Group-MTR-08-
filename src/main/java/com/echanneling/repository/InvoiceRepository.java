package com.echanneling.repository;

import com.echanneling.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Invoice i where i.id=:id")
    java.util.Optional<Invoice> lock(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(i.amount),0) from Invoice i where i.status = 'PAID'")
    java.math.BigDecimal paidRevenue();
    java.util.List<Invoice> findByAppointmentIn(java.util.Collection<com.echanneling.entity.Appointment> appointments);
    java.util.Optional<Invoice> findByAppointment(com.echanneling.entity.Appointment appointment);
}
