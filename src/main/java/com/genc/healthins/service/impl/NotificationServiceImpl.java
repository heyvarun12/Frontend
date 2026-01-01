package com.genc.healthins.service.impl;

import com.genc.healthins.model.Notification;
import com.genc.healthins.model.User;
import com.genc.healthins.repository.NotificationRepository;
import com.genc.healthins.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> findByUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public List<Notification> findUnreadByUser(User user) {
        return notificationRepository.findByUserAndIsReadFalse(user);
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        notificationRepository.deleteById(id);
    }
}