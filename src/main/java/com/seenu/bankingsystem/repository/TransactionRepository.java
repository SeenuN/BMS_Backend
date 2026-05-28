package com.seenu.bankingsystem.repository;

import com.seenu.bankingsystem.dto.TransactionResponse;
import com.seenu.bankingsystem.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    void deleteByAccountId(Long accountId);

    // Used by FraudDetectionService — finds all transactions for an account after a given time
    List<Transaction> findByAccountIdAndCreatedAtAfter(Long accountId, LocalDateTime after);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);



    @Query(value = """
    SELECT COALESCE(SUM(amount),0)
    FROM transactions
    WHERE account_id = :accountId
    AND DATE(created_at) = CURDATE()
    AND transaction_type IN ('TRANSFER_DEBIT','WITHDRAW')
    """, nativeQuery = true)
    BigDecimal getTodayDebit(Long accountId);




    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(
            Long accountId,
            Pageable pageable
    );


    @Query("""
    SELECT new com.seenu.bankingsystem.dto.TransactionResponse(
        t.id,
        a.accountNumber,
        t.amount,
        t.balanceAfter,
        t.transactionType,
        t.description,
        COALESCE(t.category, 'OTHERS'),
        t.createdAt
    )
    FROM Transaction t
    JOIN Account a ON t.accountId = a.id
""")
    Page<TransactionResponse> getAllTransactionsWithAccountNumber(Pageable pageable);

    List<Transaction> findByAccountIdInAndCreatedAtAfter(List<Long> accountIds, LocalDateTime after);
}