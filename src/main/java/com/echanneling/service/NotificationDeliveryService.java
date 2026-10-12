package com.echanneling.service;
import com.echanneling.entity.Notification;
import com.echanneling.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import static com.echanneling.entity.Notification.NotificationStatus.*;
@Service
public class NotificationDeliveryService {
    private final NotificationRepository notifications;
    private final com.echanneling.strategy.NotificationStrategy transport;
    private final AppointmentRepository appointments;
    private final TransactionTemplate transaction;
    @Value("${app.mail.enabled:false}") private boolean enabled;
    public NotificationDeliveryService(NotificationRepository notifications,
            @org.springframework.beans.factory.annotation.Qualifier("emailNotificationStrategy") com.echanneling.strategy.NotificationStrategy transport,
            AppointmentRepository appointments, org.springframework.transaction.PlatformTransactionManager manager) {
        this.notifications=notifications; this.transport=transport; this.appointments=appointments;
        this.transaction=new TransactionTemplate(manager);
    }
    public boolean isEnabled() { return enabled; }
    private record Delivery(Long id, String token, String recipient, String subject, String body) {}
    public void deliver(Long id) {
        if (!enabled) return;
        Delivery delivery=transaction.execute(s -> {
            notifications.findAppointmentId(id).ifPresent(appointmentId -> appointments.findLockedById(appointmentId));
            Notification n=notifications.findLockedById(id).orElse(null);
            LocalDateTime now=com.echanneling.patterns.ClinicClock.getInstance().now();
            if(n==null || n.getStatus()!=PENDING || (n.getNextAttemptAt()!=null && n.getNextAttemptAt().isAfter(now))
                || (n.getDeliveryLeaseUntil()!=null && n.getDeliveryLeaseUntil().isAfter(now))) return null;
            if(n.getAppointment()!=null && n.getType()!=Notification.NotificationType.CANCELLATION
                && (!"SCHEDULED".equals(n.getAppointment().getStatus()) || !n.getAppointment().getAppointmentDate().isAfter(now))) {
                n.setStatus(CANCELLED); n.setDeliveryError("The appointment is no longer upcoming."); return null;
            }
            if (n.getAttempts()>=3) { n.setStatus(FAILED); return null; }
            String token=java.util.UUID.randomUUID().toString();
            n.setDeliveryToken(token); n.setDeliveryLeaseUntil(now.plusMinutes(2));
            n.setAttempts(n.getAttempts()+1);
            return new Delivery(n.getId(),token,n.getRecipient(),n.getSubject()==null ? "CareLink notification" : n.getSubject(),n.getMessage());
        });
        if(delivery==null) return;
        boolean success;
        try { transport.sendNotification(delivery.recipient(),delivery.subject(),delivery.body()); success=true; }
        catch(Exception ex) { success=false; }
        boolean delivered=success;
        transaction.executeWithoutResult(s -> {
            Notification n=notifications.findLockedById(id).orElse(null);
            if(n==null || !delivery.token().equals(n.getDeliveryToken())) return;
            n.setDeliveryToken(null); n.setDeliveryLeaseUntil(null);
            // Cancellation or a reschedule can supersede the message while SMTP is in flight.
            if(n.getStatus()!=PENDING) return;
            if(delivered) { n.setStatus(SENT); n.setSentAt(com.echanneling.patterns.ClinicClock.getInstance().now()); n.setNextAttemptAt(null); n.setDeliveryError(null); }
            else { n.setDeliveryError("Email delivery failed. Check the sender settings and recipient address.");
                n.setStatus(n.getAttempts()>=3 ? FAILED : PENDING);
                n.setNextAttemptAt(com.echanneling.patterns.ClinicClock.getInstance().now().plusMinutes(n.getAttempts()*2L)); }
        });
    }
}
