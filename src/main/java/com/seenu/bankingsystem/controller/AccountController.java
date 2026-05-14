package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.dto.AccountResponse;
import com.seenu.bankingsystem.dto.BalanceResponse;
import com.seenu.bankingsystem.entity.Account;
import com.seenu.bankingsystem.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/create")
    public Account createAccount(
            @RequestParam Long userId,
            @RequestParam String accountType,
            @RequestParam String branch,
            @RequestParam BigDecimal balance
    ){
        return accountService.createAccount(userId, accountType, branch, balance);
    }


    @GetMapping
    public List<AccountResponse> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/balance")
    public BalanceResponse balance(
            @RequestParam String accountNumber
    ){
        return accountService.getBalance(accountNumber);
    }


    @PutMapping("/freeze")
    public Account freeze(@RequestParam String accountNumber) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountService.freezeAccount(accountNumber, username);
    }

    @PutMapping("/unfreeze")
    public Account unfreeze(@RequestParam String accountNumber) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountService.unfreezeAccount(accountNumber, username);
    }

    @PutMapping("/close")
    public Account close(@RequestParam String accountNumber) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountService.closeAccount(accountNumber, username);
    }

    @PutMapping("/update")
    public Account update(
            @RequestParam String accountNumber,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String status
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountService.updateAccount(accountNumber, accountType, branch, status, username);
    }
}