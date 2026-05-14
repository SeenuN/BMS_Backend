package com.seenu.bankingsystem.service;

import com.seenu.bankingsystem.entity.Loan;
import com.seenu.bankingsystem.repository.LoanRepository;
import com.seenu.bankingsystem.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Apply for a loan — starts as PENDING.
     */
    public Loan applyLoan(Long userId, Long accountId,
                          BigDecimal amount, Integer tenureMonths,
                          String purpose, String username) {

        // Default interest rate: 10% per annum
        BigDecimal annualRate = new BigDecimal("10.00");
        BigDecimal emi = calculateEmi(amount, annualRate, tenureMonths);

        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setAccountId(accountId);
        loan.setAmount(amount);
        loan.setInterestRate(annualRate);
        loan.setTenureMonths(tenureMonths);
        loan.setEmi(emi);
        loan.setOutstandingBalance(amount);
        loan.setStatus("PENDING");
        loan.setPurpose(purpose);

        Loan saved = loanRepository.save(loan);

        notificationService.push(userId,
                "Loan Application Received",
                "Your loan application for ₹" + amount + " is under review.",
                "LOAN");

        auditLogService.log(username, "LOAN_APPLY", "LOAN",
                saved.getId(), "Applied for ₹" + amount + " loan", "", "SUCCESS");

        return saved;
    }

    /**
     * Admin: approve a loan → disburse amount into account.
     */
    @Transactional
    public Loan approveLoan(Long loanId, String adminUsername) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!"PENDING".equals(loan.getStatus())) {
            throw new RuntimeException("Loan is not in PENDING state");
        }

        // Credit disbursement to account
        accountRepository.findById(loan.getAccountId()).ifPresent(account -> {
            account.setBalance(account.getBalance().add(loan.getAmount()));
            accountRepository.save(account);
        });

        loan.setStatus("ACTIVE");
        loan.setApprovedAt(LocalDateTime.now());
        Loan saved = loanRepository.save(loan);

        notificationService.push(loan.getUserId(),
                "Loan Approved! 🎉",
                "₹" + loan.getAmount() + " has been credited to your account.",
                "LOAN");

        auditLogService.log(adminUsername, "LOAN_APPROVE", "LOAN",
                loanId, "Approved loan ₹" + loan.getAmount(), "", "SUCCESS");

        return saved;
    }

    /**
     * Admin: reject a loan.
     */
    public Loan rejectLoan(Long loanId, String adminUsername) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        loan.setStatus("REJECTED");
        Loan saved = loanRepository.save(loan);

        notificationService.push(loan.getUserId(),
                "Loan Application Rejected",
                "Unfortunately your loan application of ₹" + loan.getAmount() + " was rejected.",
                "LOAN");

        auditLogService.log(adminUsername, "LOAN_REJECT", "LOAN",
                loanId, "Rejected loan ₹" + loan.getAmount(), "", "SUCCESS");

        return saved;
    }

    public List<Loan> getLoansForUser(Long userId) {
        return loanRepository.findByUserId(userId);
    }

    public Page<Loan> getAllLoans(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        return loanRepository.findAllByOrderByAppliedAtDesc(pageable);
    }

    /**
     * EMI formula: P * r * (1+r)^n / ((1+r)^n - 1)
     * where r = monthly interest rate
     */
    private BigDecimal calculateEmi(BigDecimal principal,
                                    BigDecimal annualRatePercent,
                                    int months) {
        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(months, new MathContext(10, RoundingMode.HALF_UP));

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
