package com.seenu.bankingsystem.service;

import com.seenu.bankingsystem.entity.Notification;
import com.seenu.bankingsystem.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /** Push a notification to a user (call from other services). */
    public void push(Long userId, String title, String message, String type) {
        notificationRepository.save(new Notification(userId, title, message, type));
    }

    public List<Notification> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    public void markOneRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }
}
