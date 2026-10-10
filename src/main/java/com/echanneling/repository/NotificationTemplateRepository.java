package com.echanneling.repository;
import com.echanneling.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate,Notification.NotificationType> {}
