package com.echanneling.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Appointment appointment;

    @jakarta.validation.constraints.NotNull(message = "Amount is required")
    @jakarta.validation.constraints.Positive(message = "Amount must be positive")
    private Double amount;
    private String status; // PAID, UNPAID, REFUNDED
    private String paymentMethod; // CARD (demonstration), CASH; null for historical invoices
    private String collectedBy;
    private LocalDateTime collectedAt;
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { paymentMethod = value; }
    public String getCollectedBy() { return collectedBy; }
    public void setCollectedBy(String value) { collectedBy = value; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime value) { collectedAt = value; }

    private LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment a) { this.appointment = a; }

    public Double getAmount() { return amount; }
    public void setAmount(Double a) { this.amount = a; }

    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
}
