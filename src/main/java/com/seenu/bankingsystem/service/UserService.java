package com.seenu.bankingsystem.service;

import com.seenu.bankingsystem.entity.Account;
import com.seenu.bankingsystem.entity.User;
import com.seenu.bankingsystem.repository.AccountRepository;
import com.seenu.bankingsystem.repository.TransactionRepository;
import com.seenu.bankingsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EmailService emailService;

    // Register new user
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRoleId() == null) user.setRoleId(2); // default: USER
        if (user.getStatus() == null) user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(Long id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(updatedUser.getName());
        user.setEmail(updatedUser.getEmail());
        user.setPhone(updatedUser.getPhone());
        user.setRoleId(updatedUser.getRoleId());
        user.setStatus(updatedUser.getStatus());

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        // 1️⃣ Find all accounts belonging to the user
        List<Account> userAccounts = accountRepository.findAllByUserId(id);

        // 2️⃣ For each account, delete all its transactions first
        for (Account account : userAccounts) {
            transactionRepository.deleteByAccountId(account.getId());
        }

        // 3️⃣ Delete the accounts
        accountRepository.deleteAll(userAccounts);

        // 4️⃣ Finally delete the user
        userRepository.deleteById(id);
    }

    public void generateAndSendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with this email"));

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // Print to console for development testing
        System.out.println("Generated OTP for " + email + " is: " + otp);

        try {
            emailService.sendOtpEmail(email, otp);
        } catch (Exception e) {
            System.err.println("Failed to send OTP email. Please check .env SMTP credentials: " + e.getMessage());
            System.err.println("Skipping email send. Use the OTP printed above for testing.");
        }
    }

    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);
    }
}
