package com.echanneling.entity;
@jakarta.persistence.Entity
public class InvoiceAudit {
    @jakarta.persistence.Id @jakarta.persistence.GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) private Long id;
    @jakarta.persistence.ManyToOne(optional=false) private Invoice invoice;
    private String oldStatus; private String newStatus; private String changedBy;
    private java.time.LocalDateTime changedAt;
    public InvoiceAudit() {}
    public InvoiceAudit(Invoice invoice,String oldStatus,String newStatus,String actor) {
        this.invoice=invoice;this.oldStatus=oldStatus;this.newStatus=newStatus;changedBy=actor;changedAt=java.time.LocalDateTime.now();
    }
}
