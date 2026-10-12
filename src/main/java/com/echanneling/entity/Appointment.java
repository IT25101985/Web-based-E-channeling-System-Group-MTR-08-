package com.echanneling.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(indexes={@Index(name="ix_appointment_doctor_time",columnList="doctor_id,appointmentDate"), @Index(name="ix_appointment_patient_time",columnList="patient_id,appointmentDate"), @Index(name="ix_appointment_recipient_time",columnList="recipientNic,appointmentDate")})
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    private Doctor doctor;

    @ManyToOne
    private HospitalBranch hospitalBranch;

    public HospitalBranch getHospitalBranch() { return hospitalBranch; }
    public void setHospitalBranch(HospitalBranch hospitalBranch) { this.hospitalBranch = hospitalBranch; }

    @ManyToOne
    private User patient;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime appointmentDate;
    private String contactPhone;
    private String contactEmail;
    private Boolean hiddenByPatient = false;
    public boolean isHiddenByPatient() { return Boolean.TRUE.equals(hiddenByPatient); }
    public void setHiddenByPatient(boolean value) { hiddenByPatient = value; }
    private String status; // SCHEDULED, CHECKED_IN, COMPLETED, CANCELLED
    private String bookingFor;
    private String recipientNic;
    private String settlementType;
    @Column(precision = 19, scale = 2)
    private java.math.BigDecimal bookedConsultationFee;

    public String getBookingFor() { return bookingFor == null ? "self" : bookingFor; }
    public void setBookingFor(String value) { bookingFor = value; }
    public boolean isForSomeoneElse() { return "other".equals(getBookingFor()); }
    public String getRecipientNic() { return recipientNic; }
    public void setRecipientNic(String value) { recipientNic = value; }
    public String getSettlementType() { return settlementType; }
    public void setSettlementType(String value) { settlementType = value; }
    public java.math.BigDecimal getBookedConsultationFee() { return bookedConsultationFee; }
    public void setBookedConsultationFee(Number value) { bookedConsultationFee = com.echanneling.service.Money.amount(value); }
    // Legacy appointments did not record an agreed fee; new bookings always do.
    public java.math.BigDecimal getPayableFee() { return bookedConsultationFee != null ? bookedConsultationFee : doctor.getConsultationFee(); }
    public boolean isAwaitingConsultation() { return "SCHEDULED".equals(status) || "CHECKED_IN".equals(status); }

    // Additional fields for database reporting/display
    private String doctorName;
    private String patientName;
    private String date;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getPatientName() { return patientName != null ? patientName : patient == null ? null : patient.getFullName(); }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public LocalDateTime getAppointmentDate() { return appointmentDate; }

    /**
     * Encapsulation demo: Simple validation
     */
    public void setAppointmentDate(LocalDateTime date) {
        this.appointmentDate = date;
    }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @PrePersist
    @PreUpdate
    public void populateNames() {
        if (doctor != null) {
            this.doctorName = doctor.getName();
        }
        if (patient != null && (patientName == null || patientName.isBlank())) {
            this.patientName = patient.getFullName();
        }
        if (appointmentDate != null) {
            this.date = appointmentDate.toLocalDate().toString();
        }
    }
}
