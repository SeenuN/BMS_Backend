package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.entity.Loan;
import com.seenu.bankingsystem.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @PostMapping("/apply")
    public Loan apply(
            @RequestParam Long userId,
            @RequestParam Long accountId,
            @RequestParam BigDecimal amount,
            @RequestParam Integer tenureMonths,
            @RequestParam(required = false, defaultValue = "Personal") String purpose
    ) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return loanService.applyLoan(userId, accountId, amount, tenureMonths, purpose, username);
    }

    @PutMapping("/{id}/approve")
    public Loan approve(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return loanService.approveLoan(id, username);
    }

    @PutMapping("/{id}/reject")
    public Loan reject(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return loanService.rejectLoan(id, username);
    }

    @GetMapping("/user/{userId}")
    public List<Loan> getForUser(@PathVariable Long userId) {
        return loanService.getLoansForUser(userId);
    }

    @GetMapping
    public Page<Loan> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return loanService.getAllLoans(page, size);
    }
}

