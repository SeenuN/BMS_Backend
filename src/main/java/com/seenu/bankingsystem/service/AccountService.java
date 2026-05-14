package com.seenu.bankingsystem.service;

import com.seenu.bankingsystem.dto.AccountResponse;
import com.seenu.bankingsystem.entity.Account;
import com.seenu.bankingsystem.dto.BalanceResponse;
import com.seenu.bankingsystem.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public BalanceResponse getBalance(String accountNumber) {
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return new BalanceResponse(account.getAccountNumber(), account.getBalance());
    }

    public Account createAccount(Long userId, String accountType, String branch, BigDecimal balance) {

        Account account = new Account();
        account.setUserId(userId);
        account.setAccountType(accountType);
        account.setBalance(balance != null ? balance : BigDecimal.ZERO);  // ✅ safe
        account.setStatus("ACTIVE");
        account.setBranch(branch != null ? branch : "Main Branch");       // ✅ safe
        account.setAccountNumber(UUID.randomUUID().toString().substring(0, 12));

        Account saved = accountRepository.save(account);

        notificationService.push(userId,
                "Account Created",
                "Your " + accountType + " account " + saved.getAccountNumber() + " is now active.",
                "SYSTEM");

        return saved;
    }
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.getAllAccountDetails();
    }

    /** Freeze an account — no transactions allowed while FROZEN. */
    public Account freezeAccount(String accountNumber, String adminUsername) {
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setStatus("FROZEN");
        Account saved = accountRepository.save(account);

        notificationService.push(account.getUserId(),
                "Account Frozen ⚠️",
                "Your account " + accountNumber + " has been frozen by admin.",
                "SECURITY");

        auditLogService.log(adminUsername, "ACCOUNT_FREEZE", "ACCOUNT",
                account.getId(), "Froze account " + accountNumber, "", "SUCCESS");

        return saved;
    }

    /** Unfreeze a previously frozen account. */
    public Account unfreezeAccount(String accountNumber, String adminUsername) {
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setStatus("ACTIVE");
        Account saved = accountRepository.save(account);

        notificationService.push(account.getUserId(),
                "Account Unfrozen ✅",
                "Your account " + accountNumber + " has been reactivated.",
                "SECURITY");

        auditLogService.log(adminUsername, "ACCOUNT_UNFREEZE", "ACCOUNT",
                account.getId(), "Unfroze account " + accountNumber, "", "SUCCESS");

        return saved;
    }

    /** Close an account permanently. */
    public Account closeAccount(String accountNumber, String adminUsername) {
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setStatus("CLOSED");
        Account saved = accountRepository.save(account);

        notificationService.push(account.getUserId(),
                "Account Closed",
                "Your account " + accountNumber + " has been permanently closed.",
                "SYSTEM");

        auditLogService.log(adminUsername, "ACCOUNT_CLOSE", "ACCOUNT",
                account.getId(), "Closed account " + accountNumber, "", "SUCCESS");

        return saved;
    }

    /** Update account details — accountType, branch, status. */
    public Account updateAccount(String accountNumber, String accountType, String branch, String status, String adminUsername) {
        Account account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (accountType != null && !accountType.isBlank()) account.setAccountType(accountType);
        if (branch != null && !branch.isBlank()) account.setBranch(branch);
        if (status != null && !status.isBlank()) account.setStatus(status);

        Account saved = accountRepository.save(account);

        auditLogService.log(adminUsername, "ACCOUNT_UPDATE", "ACCOUNT",
                account.getId(), "Updated account " + accountNumber, "", "SUCCESS");

        return saved;
    }
}
