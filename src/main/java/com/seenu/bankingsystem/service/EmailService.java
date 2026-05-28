package com.seenu.bankingsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your Password Reset OTP");
        message.setText("Dear User,\n\nYour OTP for password reset is: " + otp + "\n\nThis OTP is valid for 10 minutes.\n\nThank you,\nBanking System Team");
        
        mailSender.send(message);
    }

    /** Send an SMS-style alert via email (used by SmsService). */
    public void sendSmsViaEmail(String to, String subject, String smsBody) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(smsBody + "\n\n---\nThis is an SMS alert delivered via email.\nBMS Banking System");

        mailSender.send(message);
    }
}
