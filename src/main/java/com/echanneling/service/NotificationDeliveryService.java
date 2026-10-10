package com.echanneling.service;
import com.echanneling.entity.Notification;
import com.echanneling.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static com.echanneling.entity.Notification.NotificationStatus.*;
@Service
public class NotificationDeliveryService {
    private final NotificationRepository notifications;
    private final com.echanneling.strategy.NotificationStrategy transport;
    private final com.echanneling.repository.AppointmentRepository appointments;
    @Value("${app.mail.enabled:false}") private boolean enabled;
    public NotificationDeliveryService(NotificationRepository notifications,@org.springframework.beans.factory.annotation.Qualifier("emailNotificationStrategy") com.echanneling.strategy.NotificationStrategy transport,com.echanneling.repository.AppointmentRepository appointments) {this.notifications=notifications;this.transport=transport;this.appointments=appointments;}
    public boolean isEnabled() { return enabled; }
    // Hold the row lock through delivery so concurrent workers cannot send the same pending record together.
    @Transactional public void deliver(Long id) {
        if(!enabled) return;
        // Match the appointment -> notification lock order used by cancellation and reminders.
        notifications.findAppointmentId(id).ifPresent(appointmentId -> appointments.findLockedById(appointmentId));
        Notification n=notifications.findLockedById(id).orElse(null);
        if(n==null || n.getStatus()!=PENDING || (n.getNextAttemptAt()!=null && n.getNextAttemptAt().isAfter(LocalDateTime.now()))) return;
        if(n.getAppointment()!=null && n.getType()!=Notification.NotificationType.CANCELLATION
                && (!"SCHEDULED".equals(n.getAppointment().getStatus()) || !n.getAppointment().getAppointmentDate().isAfter(LocalDateTime.now()))) {
            n.setStatus(CANCELLED); n.setDeliveryError("The appointment is no longer upcoming."); return;
        }
        n.setAttempts(n.getAttempts()+1);
        try {
            transport.sendNotification(n.getRecipient(), n.getSubject()==null ? "CareLink notification" : n.getSubject(), n.getMessage());
            n.setStatus(SENT); n.setSentAt(LocalDateTime.now()); n.setNextAttemptAt(null); n.setDeliveryError(null);
        } catch(Exception e) {
            n.setDeliveryError("Email delivery failed. Check the sender settings and recipient address.");
            n.setStatus(n.getAttempts()>=3 ? FAILED : PENDING);
            n.setNextAttemptAt(LocalDateTime.now().plusMinutes(n.getAttempts()*2L));
        }
    }
}
