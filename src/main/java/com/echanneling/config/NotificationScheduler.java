package com.echanneling.config;
import com.echanneling.entity.Notification;
import com.echanneling.repository.*;
import com.echanneling.service.*;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
@Component
@EnableScheduling
@ConditionalOnProperty(name="app.notifications.scheduling-enabled",havingValue="true",matchIfMissing=true)
public class NotificationScheduler {
    private final NotificationRepository notifications;
    private final AppointmentRepository appointments;
    private final NotificationService service;
    private final NotificationDeliveryService delivery;
    public NotificationScheduler(NotificationRepository n,AppointmentRepository a,NotificationService s,NotificationDeliveryService d) {
        notifications=n;appointments=a;service=s;delivery=d;
    }
    @Scheduled(fixedDelayString="${app.notifications.poll-ms:30000}",initialDelayString="${app.notifications.initial-delay-ms:30000}")
    public void run() {
        LocalDateTime now=LocalDateTime.now();
        for(var a:appointments.findByStatusAndAppointmentDateBetween("SCHEDULED",now,now.plusHours(24))) {
            try { service.queueReminder(a.getId()); } catch(RuntimeException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not queue reminder for appointment {}",a.getId()); }
        }
        if(delivery.isEnabled()) for(Long id:notifications.findDueIds(Notification.NotificationStatus.PENDING,LocalDateTime.now(),PageRequest.of(0,50))) {
            try { delivery.deliver(id); } catch(RuntimeException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not process notification {}",id); }
        }
    }
}
