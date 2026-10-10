package com.echanneling.repository;

import com.echanneling.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    java.util.Optional<Invoice> findByAppointment(com.echanneling.entity.Appointment appointment);
}
