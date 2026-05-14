package com.seenu.bankingsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {

    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String status;
    private LocalDateTime createdAt;

    private String userName;
    private String branch;

    public AccountResponse(String accountNumber, String accountType,
                           BigDecimal balance, String status,
                           LocalDateTime createdAt,
                           String userName, String branch) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.userName = userName;
        this.branch = branch;
    }

    // getters
    public String getAccountNumber() { return accountNumber; }
    public String getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getUserName() { return userName; }
    public String getBranch() { return branch; }
}