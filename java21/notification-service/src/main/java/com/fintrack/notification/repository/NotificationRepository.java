package com.fintrack.notification.repository;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByUuid(String uuid);
    Page<Notification> findByAccountUuid(String accountUuid, Pageable pageable);
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
    long countByStatus(NotificationStatus status);
}
