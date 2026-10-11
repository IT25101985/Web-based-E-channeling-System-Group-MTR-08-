package com.echanneling.service;

import com.echanneling.entity.*;
import com.echanneling.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import com.echanneling.patterns.observer.*;
import com.echanneling.patterns.decorator.*;
import java.util.*;
import static com.echanneling.entity.Notification.NotificationStatus.*;
import static com.echanneling.entity.Notification.NotificationType.*;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationTemplateRepository templates;
    private final UserRepository users;
    private final AppointmentRepository appointments;
    public NotificationService(NotificationRepository notifications,NotificationTemplateRepository templates,
            UserRepository users,AppointmentRepository appointments) {
        this.notifications=notifications; this.templates=templates; this.users=users; this.appointments=appointments;
    }
    public List<Notification> getAllNotifications() { return notifications.findAllByOrderByIdDesc(); }
    public List<Notification> history(String username) { return notifications.findByRecipientUserUsernameOrderByIdDesc(username); }
    @Transactional public void markRead(Long id,String username) {
        Notification n=notifications.findLockedById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(n.getRecipientUser()==null || !n.getRecipientUser().getUsername().equals(username))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        n.setReadAt(LocalDateTime.now());
    }
    @Transactional public void saveManual(Long id,String recipient,String subject,String message,Notification.NotificationType type) {
        if(recipient==null || !recipient.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || recipient.length()>254
                || subject==null || subject.isBlank() || subject.length()>150 || subject.contains("\n") || subject.contains("\r")
                || message==null || message.isBlank() || message.length()>2000 || type==null)
            throw new IllegalArgumentException("Enter a valid email, subject (150 characters max), and message (2000 max).");
        Notification n=id==null ? new Notification() : notifications.findLockedById(id).orElseThrow();
        if(n.getStatus()==SENT || n.getStatus()==CANCELLED || n.getAppointment()!=null)
            throw new IllegalArgumentException("Only unsent manual messages can be edited.");
        n.setRecipient(recipient.trim()); n.setSubject(subject.trim()); n.setMessage(message.trim()); n.setType(type);
        n.setRecipientUser(users.findAll().stream().filter(u->u.getEmail()!=null && recipient.equalsIgnoreCase(u.getEmail().trim())).findFirst().orElse(null));
        n.setStatus(PENDING); n.setAttempts(0); n.setNextAttemptAt(null); n.setDeliveryError(null);
        notifications.save(n);
    }
    @Transactional public void retry(Long id) {
        Notification n=notifications.findLockedById(id).orElseThrow();
        if(n.getStatus()!=FAILED) throw new IllegalArgumentException("Only failed messages can be retried.");
        if(n.getAppointment()!=null && n.getType()!=CANCELLATION && !"SCHEDULED".equals(n.getAppointment().getStatus()))
            throw new IllegalArgumentException("This appointment is no longer scheduled.");
        n.setStatus(PENDING); n.setAttempts(0); n.setNextAttemptAt(null); n.setDeliveryError(null);
    }
    @Transactional public void deleteNotification(Long id) {
        Notification n=notifications.findLockedById(id).orElseThrow();
        // Keep event keys for appointment reminders so deletion cannot recreate the same reminder.
        if(n.getAppointment()!=null) { n.setStatus(CANCELLED); n.setDeliveryError("Removed from delivery queue."); }
        else notifications.delete(n);
    }
    @Transactional public void appointmentEvent(Appointment a,Notification.NotificationType type) {
        if(type==RESCHEDULE || type==CANCELLATION) {
            for(Notification n:notifications.findByAppointmentId(a.getId())) {
                if(n.getStatus()==PENDING || n.getStatus()==FAILED) n.setStatus(CANCELLED);
            }
        }
        String key=type==REMINDER ? "reminder:"+a.getId()+":"+a.getAppointmentDate() : UUID.randomUUID().toString();
        AppointmentSubject subject = new AppointmentEventSubject();
        NotificationSink sink = (event, recipient, email, eventKey) ->
            queueFor(event.appointment(), recipient, email, event.type(), eventKey);
        subject.addObserver(new PatientNotificationObserver(sink));
        subject.addObserver(new DoctorNotificationObserver(sink));
        // Observers queue messages in the caller's transaction; SMTP is still delivered by the outbox.
        subject.notifyObservers(new AppointmentEvent(a, type, key));
    }
    private void queueFor(Appointment a,User user,String email,Notification.NotificationType type,String eventKey) {
        if(notifications.existsByEventKey(eventKey)) return;
        NotificationTemplate template=template(type);
        Notification n=new Notification(email,render(template.getBody(),a,user),type,PENDING);
        n.setSubject(render(template.getSubject(),a,user)); n.setRecipientUser(user); n.setAppointment(a); n.setEventKey(eventKey);
        if(email==null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            n.setStatus(FAILED); n.setDeliveryError("No valid email address is available for this account.");
        }
        notifications.save(n);
    }
    private String render(String text,Appointment a,User user) {
        NotificationMessage message = new TemplateMessage(text);
        message = new RecipientMessageDecorator(message, user);
        message = new AppointmentMessageDecorator(message, a);
        return message.render();
    }

    public NotificationTemplate template(Notification.NotificationType type) {
        return templates.findById(type).orElseGet(()->{
            NotificationTemplate t=new NotificationTemplate(); t.setEventType(type);
            String event=switch(type) { case CONFIRMATION->"confirmed"; case RESCHEDULE->"rescheduled"; case REMINDER->"coming up"; case CANCELLATION->"cancelled"; default->"updated"; };
            t.setSubject("CareLink | Appointment "+event);
            t.setBody("Hello {name},\n\nAppointment #{reference} for {patient} with {doctor} at {hospital} on {date} is "+event+".\n\nView your appointment in CareLink for details.\nCareLink team"); return t;
        });
    }
    public List<NotificationTemplate> allTemplates() { return Arrays.stream(Notification.NotificationType.values()).map(this::template).toList(); }
    public void saveTemplate(Notification.NotificationType type,String subject,String body) {
        if(subject==null || subject.isBlank() || subject.length()>150 || subject.contains("\n") || subject.contains("\r")
                || body==null || body.isBlank() || body.length()>2000) throw new IllegalArgumentException("Enter a subject up to 150 characters and body up to 2000.");
        var matcher=java.util.regex.Pattern.compile("\\{([^}]+)}").matcher(subject+body);
        while(matcher.find()) if(!Set.of("name","patient","doctor","hospital","date","reference").contains(matcher.group(1)))
            throw new IllegalArgumentException("Unknown placeholder: "+matcher.group());
        NotificationTemplate t=new NotificationTemplate(); t.setEventType(type); t.setSubject(subject); t.setBody(body); templates.save(t);
    }
    public void resetTemplate(Notification.NotificationType type) { templates.deleteById(type); }
    @Transactional public void queueReminder(Long appointmentId) {
        Appointment a=appointments.findLockedById(appointmentId).orElse(null);
        LocalDateTime now=LocalDateTime.now();
        if(a!=null && "SCHEDULED".equals(a.getStatus()) && a.getAppointmentDate().isAfter(now) && !a.getAppointmentDate().isAfter(now.plusHours(24)))
            appointmentEvent(a,REMINDER);
    }
}
