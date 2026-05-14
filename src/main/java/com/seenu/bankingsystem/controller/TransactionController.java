package com.seenu.bankingsystem.controller;


import com.seenu.bankingsystem.dto.TransactionResponse;
import com.seenu.bankingsystem.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount
    ){
        return transactionService.deposit(accountNumber, amount);
    }


    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount
    ) {
        return transactionService.withdraw(accountNumber, amount);
    }



    @PostMapping("/transfer")
    public String transfer(
            @RequestParam String fromAccountNumber,
            @RequestParam String toAccountNumber,
            @RequestParam BigDecimal amount,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ){
        return transactionService.transfer(fromAccountNumber, toAccountNumber, amount, idempotencyKey);
    }



    @GetMapping("/statement")
    public Page<TransactionResponse> statement(
            @RequestParam String accountNumber,
            @RequestParam int page,
            @RequestParam int size
    ){
        return transactionService.getStatement(accountNumber, page, size);
    }



    @GetMapping
    public Page<TransactionResponse> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return transactionService.getAllTransactions(page, size);
    }


}