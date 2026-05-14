package com.seenu.bankingsystem.service;

import java.util.List;
import java.util.ArrayList;

import com.seenu.bankingsystem.dto.TransactionResponse;
import com.seenu.bankingsystem.entity.User;
import com.seenu.bankingsystem.repository.UserRepository;
import io.github.bucket4j.*;
import com.seenu.bankingsystem.entity.Account;
import com.seenu.bankingsystem.entity.Transaction;
import com.seenu.bankingsystem.dto.StatementResponse;
import com.seenu.bankingsystem.repository.AccountRepository;
import com.seenu.bankingsystem.repository.TransactionRepository;
import com.seenu.bankingsystem.security.RateLimiterService;
import com.seenu.bankingsystem.service.FraudDetectionService;
import com.seenu.bankingsystem.util.RequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.*;

import java.math.BigDecimal;

@Service
public class TransactionService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Transactional
    public String deposit(String accountNumber, BigDecimal amount){

        Account account = accountRepository
                .findAccountForUpdate(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            return "Account is " + account.getStatus() + ". Transaction not allowed.";
        }

        account.setBalance(account.getBalance().add(amount));

        accountRepository.save(account);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String key = username + "_DEPOSIT";
        Bucket bucket = rateLimiterService.resolveBucket(key);

        if (!bucket.tryConsume(1)) {
            return "Too many requests. Try later.";
        }

        Transaction txn = new Transaction();
        txn.setAccountId(account.getId());
        txn.setTransactionType("DEPOSIT");
        txn.setAmount(amount);
        txn.setBalanceAfter(account.getBalance());
        txn.setDescription("Deposit");

        transactionRepository.save(txn);
        fraudDetectionService.analyze(txn);   // 🔍 Fraud check
        notificationService.push(account.getUserId(),
                "Deposit Successful",
                "₹" + amount + " deposited to your account. Balance: ₹" + account.getBalance(),
                "TRANSACTION");
        String ip = RequestUtil.getClientIp();

        auditLogService.log(username, "DEPOSIT", "TRANSACTION",
                txn.getId(), "Deposited ₹" + amount, ip, "SUCCESS");

        return "Deposit successful";
    }

    @Transactional
    public String withdraw(String accountNumber, BigDecimal amount) {

        Account account = accountRepository
                .findAccountForUpdate(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            return "Account is " + account.getStatus() + ". Transaction not allowed.";
        }

        if(account.getBalance().compareTo(amount) < 0){
            return "Insufficient balance";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String key = username + "_WITHDRAW";
        Bucket bucket = rateLimiterService.resolveBucket(key);

        if (!bucket.tryConsume(1)) {
            return "Too many requests. Try later.";
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction txn = new Transaction();
        txn.setAccountId(account.getId());
        txn.setTransactionType("WITHDRAW");
        txn.setAmount(amount);
        txn.setBalanceAfter(account.getBalance());
        txn.setDescription("Withdraw money");

        transactionRepository.save(txn);
        fraudDetectionService.analyze(txn);   // 🔍 Fraud check
        notificationService.push(account.getUserId(),
                "Withdrawal Successful",
                "₹" + amount + " withdrawn. Balance: ₹" + account.getBalance(),
                "TRANSACTION");
        String ip = RequestUtil.getClientIp();
        auditLogService.log(username, "WITHDRAW", "TRANSACTION",
                txn.getId(), "Withdrew ₹" + amount, ip, "SUCCESS");

        return "Withdraw successful";
    }




    private static final BigDecimal PER_TRANSACTION_LIMIT =
            new BigDecimal("50000");

    private static final BigDecimal DAILY_LIMIT =
            new BigDecimal("200000");


    @Transactional
    public String transfer(String fromAccountNumber,
                           String toAccountNumber,
                           BigDecimal amount,
                           String idempotencyKey) {

        // duplicate request protection
        if(idempotencyKey != null && transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()){
            return "Duplicate transaction ignored";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        String key = username + "_TRANSFER";

        Bucket bucket = rateLimiterService.resolveBucket(key);

        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            return "Too many transfer requests. Try later.";
        }

        // lock sender account
        Account sender = accountRepository.findAccountForUpdate(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));

        // lock receiver account
        Account receiver = accountRepository.findAccountForUpdate(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        if (!"ACTIVE".equalsIgnoreCase(sender.getStatus())) {
            return "Sender account is " + sender.getStatus() + ". Transfer not allowed.";
        }

        if (!"ACTIVE".equalsIgnoreCase(receiver.getStatus())) {
            return "Receiver account is " + receiver.getStatus() + ". Transfer not allowed.";
        }

        if(amount.compareTo(PER_TRANSACTION_LIMIT) > 0){
            return "Per transaction limit exceeded";
        }

        BigDecimal todayDebit =
                transactionRepository.getTodayDebit(sender.getId());

        if(todayDebit.add(amount).compareTo(DAILY_LIMIT) > 0){
            return "Daily transfer limit exceeded";
        }

        // balance check
        if(sender.getBalance().compareTo(amount) < 0){
            return "Insufficient balance";
        }

        // deduct sender
        sender.setBalance(sender.getBalance().subtract(amount));
        accountRepository.save(sender);

        // credit receiver
        receiver.setBalance(receiver.getBalance().add(amount));
        accountRepository.save(receiver);

        // sender transaction
        Transaction debitTxn = new Transaction();
        debitTxn.setAccountId(sender.getId());
        debitTxn.setTransactionType("TRANSFER_DEBIT");
        debitTxn.setAmount(amount);
        debitTxn.setBalanceAfter(sender.getBalance());
        debitTxn.setDescription("Transfer to " + toAccountNumber);
        debitTxn.setIdempotencyKey(idempotencyKey);

        transactionRepository.save(debitTxn);
        fraudDetectionService.analyze(debitTxn);   // 🔍 Fraud check on sender

        // receiver transaction
        Transaction creditTxn = new Transaction();
        creditTxn.setAccountId(receiver.getId());
        creditTxn.setTransactionType("TRANSFER_CREDIT");
        creditTxn.setAmount(amount);
        creditTxn.setBalanceAfter(receiver.getBalance());
        creditTxn.setDescription("Received from " + fromAccountNumber);

        transactionRepository.save(creditTxn);

        notificationService.push(sender.getUserId(),
                "Transfer Sent",
                "₹" + amount + " transferred to " + toAccountNumber,
                "TRANSACTION");
        notificationService.push(receiver.getUserId(),
                "Money Received 💰",
                "₹" + amount + " received from " + fromAccountNumber,
                "TRANSACTION");
        String ip = RequestUtil.getClientIp();
        auditLogService.log(username, "TRANSFER", "TRANSACTION",
                debitTxn.getId(), "Transferred ₹" + amount + " to " + toAccountNumber, ip, "SUCCESS");

        return "Transfer successful";
    }


    public Page<TransactionResponse> getStatement(
            String accountNumber,
            int page,
            int size
    ) {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 🔥 SECURITY CHECK (VERY IMPORTANT)
        if (!account.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access ❌");
        }

        Pageable pageable = PageRequest.of(page, size);

        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(account.getId(), pageable)
                .map(tx -> new TransactionResponse(
                        tx.getId(),
                        account.getAccountNumber(),
                        tx.getAmount(),
                        tx.getBalanceAfter(),
                        tx.getTransactionType(),
                        tx.getDescription(),
                        tx.getCreatedAt()
                ));
    }



    public Page<TransactionResponse> getAllTransactions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.getAllTransactionsWithAccountNumber(pageable);
    }




}