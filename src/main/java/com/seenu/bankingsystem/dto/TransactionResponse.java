package com.seenu.bankingsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String accountNumber;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String transactionType;
    private String description;
    private String category;
    private LocalDateTime createdAt;

    public TransactionResponse(Long id, String accountNumber,
                               BigDecimal amount, BigDecimal balanceAfter,
                               String transactionType, String description,
                               String category,
                               LocalDateTime createdAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.transactionType = transactionType;
        this.description = description;
        this.category = category;
        this.createdAt = createdAt;
    }

    // getters

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}