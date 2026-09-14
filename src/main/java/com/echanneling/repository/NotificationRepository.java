package com.echanneling.repository;

import com.echanneling.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStatus(Notification.NotificationStatus status);
    List<Notification> findAllByOrderByIdDesc();
    List<Notification> findByRecipientContainingIgnoreCase(String recipient);
}
