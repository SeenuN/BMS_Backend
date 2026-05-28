package com.seenu.bankingsystem.repository;

import com.seenu.bankingsystem.dto.AccountResponse;
import com.seenu.bankingsystem.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByUserId(Long userId);
    List<Account> findAllByUserId(Long userId);

    @Query("""
        SELECT new com.seenu.bankingsystem.dto.AccountResponse(
            a.accountNumber,
            a.accountType,
            a.balance,
            a.status,
            a.createdAt,
            u.name,
            a.branch
        )
        FROM Account a
        JOIN User u ON a.userId = u.id
    """)
    List<AccountResponse> getAllAccountDetails();

    @Query("""
        SELECT new com.seenu.bankingsystem.dto.AccountResponse(
            a.accountNumber,
            a.accountType,
            a.balance,
            a.status,
            a.createdAt,
            u.name,
            a.branch
        )
        FROM Account a
        JOIN User u ON a.userId = u.id
        WHERE a.userId = :userId
    """)
    List<AccountResponse> getAccountDetailsByUserId(Long userId);

    // ✅ Lock for updates (transfer, withdraw)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findAccountForUpdate(String accountNumber);
}