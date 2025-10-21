package com.tanit.cto.user_management.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String sender;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.username}")
    private String mailUser;

    @Value("${app.frontend.url:/}")
    private String frontendUrl;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender, @Value("${mail.sender}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            System.out.println(">>> Using mail sender config:");
            System.out.println("Host mail: " + mailHost);
            System.out.println("User mail: " + mailUser);
            System.out.println("Sender: " + sender);
            System.out.println("ttt");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(sender);
            mailSender.send(message);

            logger.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
            e.printStackTrace();
        }
    }

    // Send account verification email
    public void sendVerificationEmail(String to, String token) {
        String link = frontendUrl + "verify?token=" + token;
        String htmlContent = """
                <p>Click the link below to verify your account:</p>
                <p><a href="%s" target="_blank" rel="noopener noreferrer">Verify your account</a></p>
                """.formatted(link);
        sendEmail(to, "Verify your account", htmlContent);
    }

    // Send password reset email
    public void sendResetPasswordEmail(String to, String token) {
        String link = frontendUrl + "reset-password?token=" + token;
        String htmlContent = """
                <p>Click the link below to reset your password:</p>
                <p><a href="%s" target="_blank" rel="noopener noreferrer">Reset your password</a></p>
                """.formatted(link);
        sendEmail(to, "Reset your password", htmlContent);
    }

}
