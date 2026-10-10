package com.echanneling.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "appointment_id", nullable = true)
    private Appointment appointment;

    private String recipient;

    @ManyToOne
    private User recipientUser;
    private String subject;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime nextAttemptAt;
    private Integer attempts = 0;
    private String deliveryError;
    private String eventKey;

    public User getRecipientUser() { return recipientUser; }
    public void setRecipientUser(User v) { recipientUser=v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { subject=v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt=v; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime v) { readAt=v; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime v) { nextAttemptAt=v; }
    public Integer getAttempts() { return attempts == null ? 0 : attempts; }
    public void setAttempts(Integer v) { attempts=v; }
    public String getDeliveryError() { return deliveryError; }
    public void setDeliveryError(String v) { deliveryError=v; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String v) { eventKey=v; }

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type = NotificationType.CONFIRMATION;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    private LocalDateTime sentAt;

    public enum NotificationType {
        CONFIRMATION, RESCHEDULE, REMINDER, CANCELLATION, SYSTEM, GENERAL
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, CANCELLED
    }

    public Notification() {}

    public Notification(String recipient, String message, NotificationType type, NotificationStatus status) {
        this.recipient = recipient;
        this.message = message;
        this.type = type;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        if (status == NotificationStatus.SENT) this.sentAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}

