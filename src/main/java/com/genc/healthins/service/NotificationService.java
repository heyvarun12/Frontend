package com.genc.healthins.service;

import com.genc.healthins.model.Notification;
import com.genc.healthins.model.User;
import java.util.List;

public interface NotificationService {
    Notification save(Notification notification);
    List<Notification> findByUser(User user);
    List<Notification> findUnreadByUser(User user);
    void markAsRead(Long id);
    void deleteById(Long id);
}