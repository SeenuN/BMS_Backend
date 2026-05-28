package com.seenu.bankingsystem.service;

import com.seenu.bankingsystem.entity.Account;
import com.seenu.bankingsystem.entity.SmsLog;
import com.seenu.bankingsystem.entity.SmsPreference;
import com.seenu.bankingsystem.entity.User;
import com.seenu.bankingsystem.repository.SmsLogRepository;
import com.seenu.bankingsystem.repository.SmsPreferenceRepository;
import com.seenu.bankingsystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final String BANK_NAME = "BMS Bank";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

    @Autowired
    private SmsLogRepository smsLogRepository;

    @Autowired
    private SmsPreferenceRepository smsPreferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    // ══════════════════════════════════════════════════════════════════════════
    //  PUBLIC METHODS — called by other services
    // ══════════════════════════════════════════════════════════════════════════

    /** SMS alert after a successful deposit. */
    public void sendDepositAlert(Account account, BigDecimal amount,
                                  BigDecimal balance, Long txnId) {
        User user = findUser(account.getUserId());
        if (user == null || !isEnabled(user.getId(), "TRANSACTION")) return;

        String msg = String.format(
                "Dear %s, %s credited to A/c XX%s. Avl Bal: %s. Ref: TXN%d. -%s",
                user.getName(),
                formatCurrency(amount),
                maskAccount(account.getAccountNumber()),
                formatCurrency(balance),
                txnId,
                BANK_NAME
        );

        dispatch(user, msg, "TRANSACTION");
    }

    /** SMS alert after a successful withdrawal. */
    public void sendWithdrawalAlert(Account account, BigDecimal amount,
                                     BigDecimal balance, Long txnId) {
        User user = findUser(account.getUserId());
        if (user == null || !isEnabled(user.getId(), "TRANSACTION")) return;

        String msg = String.format(
                "Dear %s, %s debited from A/c XX%s. Avl Bal: %s. Ref: TXN%d. -%s",
                user.getName(),
                formatCurrency(amount),
                maskAccount(account.getAccountNumber()),
                formatCurrency(balance),
                txnId,
                BANK_NAME
        );

        dispatch(user, msg, "TRANSACTION");
    }

    /** SMS alert to the sender after a transfer. */
    public void sendTransferDebitAlert(Account sender, String toAccountNumber,
                                       BigDecimal amount, BigDecimal balance,
                                       Long txnId) {
        User user = findUser(sender.getUserId());
        if (user == null || !isEnabled(user.getId(), "TRANSACTION")) return;

        String msg = String.format(
                "Dear %s, %s transferred to A/c XX%s from A/c XX%s. Avl Bal: %s. Ref: TXN%d. -%s",
                user.getName(),
                formatCurrency(amount),
                maskAccount(toAccountNumber),
                maskAccount(sender.getAccountNumber()),
                formatCurrency(balance),
                txnId,
                BANK_NAME
        );

        dispatch(user, msg, "TRANSACTION");
    }

    /** SMS alert to the receiver after a transfer. */
    public void sendTransferCreditAlert(Account receiver, String fromAccountNumber,
                                         BigDecimal amount, BigDecimal balance,
                                         Long txnId) {
        User user = findUser(receiver.getUserId());
        if (user == null || !isEnabled(user.getId(), "TRANSACTION")) return;

        String msg = String.format(
                "Dear %s, %s received in A/c XX%s from A/c XX%s. Avl Bal: %s. Ref: TXN%d. -%s",
                user.getName(),
                formatCurrency(amount),
                maskAccount(receiver.getAccountNumber()),
                maskAccount(fromAccountNumber),
                formatCurrency(balance),
                txnId,
                BANK_NAME
        );

        dispatch(user, msg, "TRANSACTION");
    }

    /** SMS alert on user login. */
    public void sendLoginAlert(User user, String ipAddress) {
        if (!isEnabled(user.getId(), "SECURITY")) return;

        String msg = String.format(
                "Dear %s, login detected on your account at %s from IP %s. If not you, call 1800-XXX-XXXX immediately. -%s",
                user.getName(),
                LocalDateTime.now().format(TIME_FMT),
                ipAddress,
                BANK_NAME
        );

        dispatch(user, msg, "SECURITY");
    }

    /** SMS alert when fraud is detected. */
    public void sendFraudAlert(Long accountId, String alertType,
                                String description) {
        // We need to look up the account's user
        // Import AccountRepository here would create circular dependency,
        // so we accept the userId directly in an overload
    }

    /** SMS alert when fraud is detected (with userId). */
    public void sendFraudAlertToUser(Long userId, String accountNumber,
                                      String alertType, String description) {
        User user = findUser(userId);
        if (user == null || !isEnabled(userId, "FRAUD")) return;

        String msg = String.format(
                "\u26A0\uFE0F ALERT: %s detected on A/c XX%s. %s. Contact bank immediately at 1800-XXX-XXXX. -%s",
                formatAlertType(alertType),
                maskAccount(accountNumber),
                description,
                BANK_NAME
        );

        dispatch(user, msg, "FRAUD");
    }

    /** SMS-based OTP delivery. */
    public void sendOtpSms(User user, String otp) {
        if (!isEnabled(user.getId(), "OTP")) return;

        String msg = String.format(
                "Dear %s, your OTP is %s. Valid for 10 minutes. Do NOT share this with anyone. -%s",
                user.getName(),
                otp,
                BANK_NAME
        );

        dispatch(user, msg, "OTP");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SMS HISTORY & PREFERENCES
    // ══════════════════════════════════════════════════════════════════════════

    public List<SmsLog> getHistory(Long userId) {
        return smsLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<SmsLog> getAllHistory() {
        return smsLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public SmsPreference getPreferences(Long userId) {
        return smsPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Create default preferences for user
                    SmsPreference pref = new SmsPreference(userId);
                    return smsPreferenceRepository.save(pref);
                });
    }

    public SmsPreference updatePreferences(Long userId, Boolean transactionAlerts,
                                            Boolean securityAlerts, Boolean fraudAlerts,
                                            Boolean otpAlerts) {
        SmsPreference pref = getPreferences(userId);
        if (transactionAlerts != null) pref.setTransactionAlerts(transactionAlerts);
        if (securityAlerts != null) pref.setSecurityAlerts(securityAlerts);
        if (fraudAlerts != null) pref.setFraudAlerts(fraudAlerts);
        if (otpAlerts != null) pref.setOtpAlerts(otpAlerts);
        return smsPreferenceRepository.save(pref);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Dispatches the SMS message via available channels:
     * 1. Console log (always)
     * 2. Email (if user has email configured)
     * Logs the result to sms_logs table.
     */
    private void dispatch(User user, String message, String smsType) {
        String phone = user.getPhone() != null ? user.getPhone() : "N/A";
        String status = "SENT";
        String channel = "CONSOLE";

        // 1️⃣ Always log to console
        log.info("📱 SMS [{}] → {} ({}): {}", smsType, user.getName(), phone, message);

        // 2️⃣ Send via email as SMS notification
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendSmsViaEmail(
                        user.getEmail(),
                        "BMS Bank Alert - " + smsType,
                        message
                );
                channel = "EMAIL";
                log.info("✉️ SMS sent via email to {}", user.getEmail());
            } catch (Exception e) {
                log.warn("⚠️ Failed to send SMS via email to {}: {}", user.getEmail(), e.getMessage());
                status = "FAILED";
            }
        }

        // 3️⃣ Save to SMS log
        SmsLog smsLog = new SmsLog(user.getId(), phone, message, smsType, status, channel);
        smsLogRepository.save(smsLog);
    }

    /** Check if a specific SMS type is enabled for this user. */
    private boolean isEnabled(Long userId, String type) {
        SmsPreference pref = smsPreferenceRepository.findByUserId(userId)
                .orElse(null);

        // If no preferences set, default to enabled
        if (pref == null) return true;

        return switch (type) {
            case "TRANSACTION" -> Boolean.TRUE.equals(pref.getTransactionAlerts());
            case "SECURITY"    -> Boolean.TRUE.equals(pref.getSecurityAlerts());
            case "FRAUD"       -> Boolean.TRUE.equals(pref.getFraudAlerts());
            case "OTP"         -> Boolean.TRUE.equals(pref.getOtpAlerts());
            default            -> true;
        };
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    /** Mask account number: show only last 4 digits. */
    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return accountNumber.substring(accountNumber.length() - 4);
    }

    /** Format amount as Indian currency: ₹1,23,456.00 */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return nf.format(amount);
    }

    /** Convert alert type codes to readable text. */
    private String formatAlertType(String alertType) {
        if (alertType == null) return "Suspicious activity";
        return switch (alertType) {
            case "HIGH_AMOUNT"         -> "Unusually high transaction";
            case "RAPID_TRANSACTIONS"  -> "Rapid consecutive transactions";
            case "ODD_HOUR"            -> "Unusual transaction time";
            default                    -> "Suspicious activity";
        };
    }
}
