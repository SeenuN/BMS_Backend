package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.dto.LoginRequest;
import com.seenu.bankingsystem.dto.LoginResponse;
import com.seenu.bankingsystem.entity.User;
import com.seenu.bankingsystem.repository.AccountRepository;
import com.seenu.bankingsystem.security.JwtUtil;
import com.seenu.bankingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountRepository accountRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User saved = userService.registerUser(user);
            return ResponseEntity.ok(Map.of(
                "userId", saved.getId(),
                "name", saved.getName(),
                "email", saved.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

        LoginResponse response = new LoginResponse();

        User user;
        try {
            user = userService.findByEmail(request.getEmail());
        } catch (RuntimeException e) {
            response.setMessage("Invalid username or password");
            return ResponseEntity.status(404).body(response);
        }

        if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            String token = jwtUtil.generateToken(user.getEmail(), user.getRoleId());
            String role = (user.getRoleId() == 1) ? "ADMIN" : "USER";

            response.setToken(token);
            response.setRole(role);
            
            // 🔥 Inject user and accountNumber so frontend knows who logged in!
            response.setUser(user);
            accountRepository.findByUserId(user.getId()).ifPresent(acc -> 
                response.setAccountNumber(acc.getAccountNumber())
            );

            return ResponseEntity.ok(response);
        }
        response.setMessage("Invalid username or password");
        return ResponseEntity.status(401).body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            userService.generateAndSendOtp(request.get("email"));
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully to your email"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            userService.resetPassword(request.get("email"), request.get("otp"), request.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "User deleted successfully";
    }
}