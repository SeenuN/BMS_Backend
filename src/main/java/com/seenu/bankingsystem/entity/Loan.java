package com.seenu.bankingsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "account_id")
    private Long accountId;

    private BigDecimal amount;

    @Column(name = "interest_rate")
    private BigDecimal interestRate;   // annual %

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    // EMI = amount * monthly_rate / (1 - (1 + monthly_rate)^-tenure)
    private BigDecimal emi;

    @Column(name = "outstanding_balance")
    private BigDecimal outstandingBalance;

    private String status;   // PENDING, APPROVED, REJECTED, ACTIVE, CLOSED

    private String purpose;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public Loan() {}

    // Getters & Setters
    public Long getId()
    {
        return id;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
    public BigDecimal getEmi() { return emi; }
    public void setEmi(BigDecimal emi) { this.emi = emi; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
