package com.seenu.bankingsystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sms_preferences")
public class SmsPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "transaction_alerts")
    private Boolean transactionAlerts = true;

    @Column(name = "security_alerts")
    private Boolean securityAlerts = true;

    @Column(name = "fraud_alerts")
    private Boolean fraudAlerts = true;

    @Column(name = "otp_alerts")
    private Boolean otpAlerts = true;

    public SmsPreference() {}

    public SmsPreference(Long userId) {
        this.userId = userId;
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Boolean getTransactionAlerts() { return transactionAlerts; }
    public Boolean getSecurityAlerts() { return securityAlerts; }
    public Boolean getFraudAlerts() { return fraudAlerts; }
    public Boolean getOtpAlerts() { return otpAlerts; }

    // Setters
    public void setUserId(Long userId) { this.userId = userId; }
    public void setTransactionAlerts(Boolean transactionAlerts) { this.transactionAlerts = transactionAlerts; }
    public void setSecurityAlerts(Boolean securityAlerts) { this.securityAlerts = securityAlerts; }
    public void setFraudAlerts(Boolean fraudAlerts) { this.fraudAlerts = fraudAlerts; }
    public void setOtpAlerts(Boolean otpAlerts) { this.otpAlerts = otpAlerts; }
}
