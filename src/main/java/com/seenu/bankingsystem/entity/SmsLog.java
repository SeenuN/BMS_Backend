package com.seenu.bankingsystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_logs")
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(length = 500)
    private String message;

    @Column(name = "sms_type")
    private String smsType;   // TRANSACTION, SECURITY, OTP, FRAUD

    private String status;    // SENT, FAILED, PENDING

    private String channel;   // EMAIL, TWILIO, CONSOLE

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public SmsLog() {}

    public SmsLog(Long userId, String phoneNumber, String message,
                  String smsType, String status, String channel) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.smsType = smsType;
        this.status = status;
        this.channel = channel;
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessage() { return message; }
    public String getSmsType() { return smsType; }
    public String getStatus() { return status; }
    public String getChannel() { return channel; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setUserId(Long userId) { this.userId = userId; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setMessage(String message) { this.message = message; }
    public void setSmsType(String smsType) { this.smsType = smsType; }
    public void setStatus(String status) { this.status = status; }
    public void setChannel(String channel) { this.channel = channel; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
