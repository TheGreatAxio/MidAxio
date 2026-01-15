package com.axios.midaxio.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("midaxio@axioscomputers.com");
        message.setTo(to);
        message.setSubject("Verify your MidAxio Account");

        message.setText("Welcome! Please verify your account here: " +
                "https://axioscomputers.com/verify?token=" + token);

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("midaxio@axioscomputers.com");
        message.setTo(to);
        message.setSubject("Reset Your MidAxio Password");

        message.setText("To reset your password, please click the link below: " +
                "https://axioscomputers.com/resetpassword?token=" + token + "&email=" + to);

        mailSender.send(message);
    }
}