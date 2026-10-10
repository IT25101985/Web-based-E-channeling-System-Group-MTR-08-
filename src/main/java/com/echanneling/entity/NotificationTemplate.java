package com.echanneling.entity;
import jakarta.persistence.*;
@Entity
public class NotificationTemplate {
    @Id @Enumerated(EnumType.STRING)
    private Notification.NotificationType eventType;
    @Column(nullable=false,length=150)
    private String subject;
    @Column(nullable=false,length=2000)
    private String body;
    public Notification.NotificationType getEventType() { return eventType; }
    public void setEventType(Notification.NotificationType v) { eventType=v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { subject=v; }
    public String getBody() { return body; }
    public void setBody(String v) { body=v; }
}
