package com.seenu.bankingsystem.service;

import com.seenu.bankingsystem.entity.FraudAlert;
import com.seenu.bankingsystem.entity.Transaction;
import com.seenu.bankingsystem.repository.FraudAlertRepository;
import com.seenu.bankingsystem.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {

    // ── Thresholds ────────────────────────────────────────────────────────────
    /** Flag any single transaction above this amount */
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("50000");

    /** Flag if the same account makes more than this many transactions in the window */
    private static final int RAPID_TX_COUNT = 5;

    /** Look-back window in minutes for rapid transaction check */
    private static final int RAPID_TX_WINDOW_MINUTES = 10;

    /** Flag transactions between midnight and 5 AM local time */
    private static final int ODD_HOUR_START = 0;
    private static final int ODD_HOUR_END   = 5;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Entry point — call this from TransactionService after every saved transaction.
     */
    public void analyze(Transaction transaction) {
        checkHighAmount(transaction);
        checkRapidTransactions(transaction);
        checkOddHour(transaction);
    }

    // ── Rules ─────────────────────────────────────────────────────────────────

    private void checkHighAmount(Transaction tx) {
        if (tx.getAmount() != null && tx.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            saveAlert(tx, "HIGH_AMOUNT",
                    "Unusually high transaction of \u20b9" + tx.getAmount() + " detected on account #" + tx.getAccountId());
        }
    }

    private void checkRapidTransactions(Transaction tx) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RAPID_TX_WINDOW_MINUTES);
        List<Transaction> recentTx = transactionRepository
                .findByAccountIdAndCreatedAtAfter(tx.getAccountId(), windowStart);

        if (recentTx.size() >= RAPID_TX_COUNT) {
            saveAlert(tx, "RAPID_TRANSACTIONS",
                    recentTx.size() + " transactions detected in the last "
                            + RAPID_TX_WINDOW_MINUTES + " minutes on account #" + tx.getAccountId());
        }
    }

    private void checkOddHour(Transaction tx) {
        int hour = LocalDateTime.now().getHour();
        if (hour >= ODD_HOUR_START && hour < ODD_HOUR_END) {
            saveAlert(tx, "ODD_HOUR",
                    "Transaction made at odd hour (" + hour + ":xx) on account #" + tx.getAccountId());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void saveAlert(Transaction tx, String type, String description) {
        FraudAlert alert = new FraudAlert();
        alert.setAccountId(tx.getAccountId());
        alert.setAlertType(type);
        alert.setDescription(description);
        alert.setTransactionAmount(tx.getAmount());
        alert.setStatus("PENDING");
        fraudAlertRepository.save(alert);
    }
}
