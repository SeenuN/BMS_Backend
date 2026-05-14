package com.seenu.bankingsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StatementResponse {

    private LocalDateTime date;
    private String description;
    private String reference;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;

    public StatementResponse(LocalDateTime date,
                             String description,
                             String reference,
                             BigDecimal debit,
                             BigDecimal credit,
                             BigDecimal balance) {

        this.date = date;
        this.description = description;
        this.reference = reference;
        this.debit = debit;
        this.credit = credit;
        this.balance = balance;
    }

    public LocalDateTime getDate() { return date; }
    public String getDescription() { return description; }
    public String getReference() { return reference; }
    public BigDecimal getDebit() { return debit; }
    public BigDecimal getCredit() { return credit; }
    public BigDecimal getBalance() { return balance; }
}