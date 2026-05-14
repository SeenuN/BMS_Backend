package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.entity.Notification;
import com.seenu.bankingsystem.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public List<Notification> getForUser(@PathVariable Long userId) {
        return notificationService.getForUser(userId);
    }

    @GetMapping("/user/{userId}/unread-count")
    public Map<String, Long> unreadCount(@PathVariable Long userId) {
        return Map.of("count", notificationService.getUnreadCount(userId));
    }

    @PutMapping("/user/{userId}/mark-all-read")
    public void markAllRead(@PathVariable Long userId) {
        notificationService.markAllRead(userId);
    }

    @PutMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationService.markOneRead(id);
    }
}
