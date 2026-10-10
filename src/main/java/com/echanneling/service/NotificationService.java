package com.echanneling.service;

import com.echanneling.entity.Notification;
import com.echanneling.repository.NotificationRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationRepository notificationRepository;

    // Dummy Twilio Credentials
    public static final String ACCOUNT_SID = "AC_DUMMY_SID";
    public static final String AUTH_TOKEN = "DUMMY_AUTH_TOKEN";
    public static final String TWILIO_NUMBER = "+1234567890";

    public NotificationService() {
        // Initialize Twilio
        // Twilio.init(ACCOUNT_SID, AUTH_TOKEN); // Commented out to prevent crash with dummy credentials
    }

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("emailNotificationStrategy")
    private com.echanneling.strategy.NotificationStrategy emailStrategy;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("smsNotificationStrategy")
    private com.echanneling.strategy.NotificationStrategy smsStrategy;

    // --- DB CRUD OPERATIONS ---

    public Notification saveNotification(Notification notification) {
        if (notification.getRecipient() == null || notification.getRecipient().isBlank() || notification.getMessage() == null || notification.getMessage().isBlank()) throw new IllegalArgumentException("Recipient and message are required");
        if (notification.getSentAt() == null) {
            notification.setSentAt(LocalDateTime.now());
        }
        return notificationRepository.save(notification);
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByIdDesc();
    }

    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    public void deleteNotification(Long id) {
        if (id != null && notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
        }
    }

    public Notification updateNotificationStatus(Long id, Notification.NotificationStatus status) {
        Optional<Notification> opt = notificationRepository.findById(id);
        if (opt.isPresent()) {
            Notification n = opt.get();
            n.setStatus(status);
            return notificationRepository.save(n);
        }
        return null;
    }

    // Strategy Execution Method
    public void notifyUser(com.echanneling.strategy.NotificationStrategy strategy, String recipient, String subject, String body) {
        strategy.sendNotification(recipient, subject, body);
    }

    public void sendBookingConfirmationEmail(String toEmail, String patientName, String dateStr) {
        String subject = "Appointment Confirmation - HealthCare Plus";
        String body = "Dear " + patientName + ",\n\nYour appointment has been successfully booked for " + dateStr + ".\n\nThank you for choosing HealthCare Plus.";
        
        Notification.NotificationStatus status = Notification.NotificationStatus.SENT;
        try {
            notifyUser(emailStrategy, toEmail, subject, body);
        } catch (Exception e) {
            status = Notification.NotificationStatus.FAILED;
            System.err.println("Failed to send email: " + e.getMessage());
        }

        Notification notification = new Notification(toEmail, body, Notification.NotificationType.CONFIRMATION, status);
        saveNotification(notification);
    }

    public void sendCancellationEmail(String toEmail, String patientName, String dateStr) {
        String subject = "Appointment Cancelled - HealthCare Plus";
        String body = "Dear " + patientName + ",\n\nWe are sorry to inform you that your appointment on " + dateStr + " has been cancelled.\n\nPlease contact us to reschedule.";
        
        Notification.NotificationStatus status = Notification.NotificationStatus.SENT;
        try {
            notifyUser(emailStrategy, toEmail, subject, body);
        } catch (Exception e) {
            status = Notification.NotificationStatus.FAILED;
            System.err.println("Failed to send cancellation email: " + e.getMessage());
        }

        Notification notification = new Notification(toEmail, body, Notification.NotificationType.CANCELLATION, status);
        saveNotification(notification);
    }

    public void sendSmsReminder(String toPhone, String patientName, String dateStr) {
        String body = "Reminder: Dear " + patientName + ", you have an appointment at HealthCare Plus on " + dateStr;
        Notification.NotificationStatus status = Notification.NotificationStatus.SENT;
        try {
            System.out.println("Mock SMS sent to " + toPhone + " for appointment on " + dateStr);
        } catch (Exception e) {
            status = Notification.NotificationStatus.FAILED;
            System.err.println("Failed to send SMS: " + e.getMessage());
        }

        Notification notification = new Notification(toPhone, body, Notification.NotificationType.REMINDER, status);
        saveNotification(notification);
    }
}

