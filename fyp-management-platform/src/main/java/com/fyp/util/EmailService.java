package com.fyp.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * JavaMail service for sending OTP emails and system notifications.
 * Configure via config.properties: mail.host, mail.port, mail.user, mail.password
 */
public class EmailService {

    private static Properties mailProps;
    private static String mailUser;
    private static String mailPassword;
    private static String mailFrom;
    private static boolean stubMode = false;

    static {
        try {
            Properties config = loadConfig();
            mailUser     = config.getProperty("mail.user", "");
            mailPassword = config.getProperty("mail.password", "");
            mailFrom     = config.getProperty("mail.from", mailUser);

            mailProps = new Properties();
            mailProps.put("mail.smtp.auth", "true");
            mailProps.put("mail.smtp.starttls.enable", "true");
            mailProps.put("mail.smtp.host", config.getProperty("mail.host", "smtp.gmail.com"));
            mailProps.put("mail.smtp.port", config.getProperty("mail.port", "587"));
            mailProps.put("mail.smtp.ssl.trust", "*");

            if (mailUser.isBlank() || mailPassword.isBlank() ||
                    mailUser.startsWith("your-") || mailPassword.startsWith("your-")) {
                stubMode = true;
                System.out.println("[EmailService] Stub mode active — emails will be printed to console.");
            }
        } catch (Exception e) {
            stubMode = true;
            System.err.println("[EmailService] Config load failed, using stub mode: " + e.getMessage());
        }
    }

    private EmailService() {}

    private static Properties loadConfig() throws IOException {
        Properties props = new Properties();
        java.io.File file = new java.io.File("config.properties");
        if (file.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                props.load(fis);
                return props;
            }
        }
        try (InputStream is = EmailService.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        }
        return props;
    }

    /**
     * Send a 6-digit OTP to the given email address.
     */
    public static void sendOTP(String toEmail, String otp) {
        String subject = "FYP Platform — Your Verification Code";
        String body = """
                Dear User,

                Your one-time verification code is:

                    %s

                This code expires in 15 minutes. Do not share it with anyone.

                — FYP Management Platform, FAST NUCES Peshawar
                """.formatted(otp);
        send(toEmail, subject, body);
    }

    /**
     * Send a general notification email.
     */
    public static void sendNotification(String toEmail, String subject, String body) {
        send(toEmail, subject, body);
    }

    private static void send(String toEmail, String subject, String body) {
        if (stubMode) {
            System.out.println("===== [EMAIL STUB] =====");
            System.out.println("To:      " + toEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Body:\n"   + body);
            System.out.println("========================");
            return;
        }

        // Send on background thread to avoid blocking UI
        Thread.ofVirtual().start(() -> {
            try {
                Session session = Session.getInstance(mailProps, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(mailUser, mailPassword);
                    }
                });

                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(mailFrom));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                msg.setSubject(subject);
                msg.setText(body);
                Transport.send(msg);
                System.out.println("[EmailService] Email sent to " + toEmail);
            } catch (MessagingException e) {
                System.err.println("[EmailService] Failed to send email: " + e.getMessage());
            }
        });
    }
}
