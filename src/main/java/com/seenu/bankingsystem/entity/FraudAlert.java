package com.seenu.bankingsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long accountId;

    // "HIGH_AMOUNT" | "RAPID_TRANSACTIONS" | "ODD_HOUR"
    private String alertType;

    private String description;

    private BigDecimal transactionAmount;

    // "PENDING" | "REVIEWED" | "DISMISSED"
    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Getters ──────────────────────────────────────────────────────────────
    public Long getId()                        { return id; }
    public Long getAccountId()                 { return accountId; }
    public String getAlertType()               { return alertType; }
    public String getDescription()             { return description; }
    public BigDecimal getTransactionAmount()   { return transactionAmount; }
    public String getStatus()                  { return status; }
    public LocalDateTime getCreatedAt()        { return createdAt; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setAccountId(Long accountId)               { this.accountId = accountId; }
    public void setAlertType(String alertType)             { this.alertType = alertType; }
    public void setDescription(String description)         { this.description = description; }
    public void setTransactionAmount(BigDecimal amount)    { this.transactionAmount = amount; }
    public void setStatus(String status)                   { this.status = status; }
}
