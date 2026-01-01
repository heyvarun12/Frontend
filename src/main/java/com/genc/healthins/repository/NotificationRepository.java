package com.genc.healthins.repository;

import com.genc.healthins.model.Notification;
import com.genc.healthins.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Find all notifications for a specific user, sorted by newest first
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    
    // Find only unread notifications for a user
    List<Notification> findByUserAndIsReadFalse(User user);
}