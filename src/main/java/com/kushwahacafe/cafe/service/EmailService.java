package com.kushwahacafe.cafe.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String smtpEmail;

    public boolean sendEmailNotification(String recipientEmail, String subject, String bodyHtml) {
        if (mailSender != null && smtpEmail != null && !smtpEmail.trim().isEmpty()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(smtpEmail);
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(bodyHtml, true);

                mailSender.send(message);
                System.out.println("[SMTP SUCCESS] Real email sent to " + recipientEmail);
                return true;
            } catch (Exception e) {
                System.err.println("[SMTP ERROR] Failed to send real email: " + e.getMessage());
            }
        }

        // Fallback to Simulation
        printEmailSimulation(recipientEmail, subject, bodyHtml);
        return true;
    }

    private void printEmailSimulation(String recipientEmail, String subject, String bodyHtml) {
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("\n" + "=".repeat(30) + " EMAIL SIMULATION " + "=".repeat(30));
        System.out.println("Recipient: " + recipientEmail);
        System.out.println("Subject:   " + subject);
        System.out.println("Timestamp: " + nowStr);
        System.out.println("-".repeat(78));
        // Clean html representation for terminal print (stripping tags for readability or printing as is)
        System.out.println(bodyHtml);
        System.out.println("=".repeat(78) + "\n");
    }
}
