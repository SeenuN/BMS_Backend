package com.seenu.bankingsystem.dto;

public class LoginResponse {

    private String token;
    private String role;
    private com.seenu.bankingsystem.entity.User user;
    private String accountNumber;
    private String message;

    // getters and setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public com.seenu.bankingsystem.entity.User getUser() {
        return user;
    }

    public void setUser(com.seenu.bankingsystem.entity.User user) {
        this.user = user;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}