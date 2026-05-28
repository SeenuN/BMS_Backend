package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.entity.SmsLog;
import com.seenu.bankingsystem.entity.SmsPreference;
import com.seenu.bankingsystem.entity.User;
import com.seenu.bankingsystem.repository.UserRepository;
import com.seenu.bankingsystem.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @Autowired
    private UserRepository userRepository;

    /** Get SMS history for the logged-in user. */
    @GetMapping("/history")
    public List<SmsLog> getMyHistory() {
        User user = getAuthenticatedUser();
        return smsService.getHistory(user.getId());
    }

    /** Admin: Get all SMS logs. */
    @GetMapping("/history/all")
    public List<SmsLog> getAllHistory() {
        return smsService.getAllHistory();
    }

    /** Get SMS preferences for the logged-in user. */
    @GetMapping("/preferences")
    public SmsPreference getPreferences() {
        User user = getAuthenticatedUser();
        return smsService.getPreferences(user.getId());
    }

    /** Update SMS preferences for the logged-in user. */
    @PutMapping("/preferences")
    public SmsPreference updatePreferences(@RequestBody Map<String, Boolean> body) {
        User user = getAuthenticatedUser();
        return smsService.updatePreferences(
                user.getId(),
                body.get("transactionAlerts"),
                body.get("securityAlerts"),
                body.get("fraudAlerts"),
                body.get("otpAlerts")
        );
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
