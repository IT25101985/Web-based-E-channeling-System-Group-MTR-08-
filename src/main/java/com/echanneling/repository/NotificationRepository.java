package com.echanneling.repository;

import com.echanneling.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select n from Notification n where n.id = :id")
    java.util.Optional<Notification> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Notification> findByRecipientUserUsernameOrderByIdDesc(String username);
    List<Notification> findByAppointmentId(Long appointmentId);
    boolean existsByEventKey(String eventKey);
    @org.springframework.data.jpa.repository.Query("select n.appointment.id from Notification n where n.id = :id")
    java.util.Optional<Long> findAppointmentId(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select n.id from Notification n where n.status = :status and (n.nextAttemptAt is null or n.nextAttemptAt <= :now) and (n.deliveryLeaseUntil is null or n.deliveryLeaseUntil <= :now) order by n.id")
    List<Long> findDueIds(@org.springframework.data.repository.query.Param("status") Notification.NotificationStatus status, @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now, org.springframework.data.domain.Pageable pageable);
    List<Notification> findByStatus(Notification.NotificationStatus status);
    List<Notification> findAllByOrderByIdDesc();
    List<Notification> findByRecipientContainingIgnoreCase(String recipient);
}
