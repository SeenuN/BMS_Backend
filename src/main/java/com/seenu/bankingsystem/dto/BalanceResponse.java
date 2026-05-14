package com.seenu.bankingsystem.dto;

import java.math.BigDecimal;

public class BalanceResponse {

    private String accountNumber;
    private BigDecimal balance;

    public BalanceResponse(String accountNumber, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}