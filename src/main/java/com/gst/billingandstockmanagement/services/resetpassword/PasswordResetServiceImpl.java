package com.gst.billingandstockmanagement.services.resetpassword;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.gst.billingandstockmanagement.services.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gst.billingandstockmanagement.entities.PasswordResetToken;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.PasswordResetTokenRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 15;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${app.client.reset-password-url}")
    private String resetPasswordUrl;

    @Override
    public void createAndSendResetToken(String email) {
        User user = userRepository.findFirstByEmail(email);

        if (user == null) {
            return;
        }

        tokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryTime(
                LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES)
        );

        tokenRepository.save(resetToken);

        sendResetEmail(user.getEmail(), token);
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository
                .findByToken(token)
                .orElse(null);

        if (resetToken == null) {
            return false;
        }

        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            return false;
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
        return true;
    }

    private void sendResetEmail(String email, String token) {
        String resetLink = resetPasswordUrl + "?token=" + token;

        // Generate unique timestamp to prevent Gmail threading
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm:ss a z");
        String formattedDateTime = now.format(formatter.withZone(ZoneId.systemDefault()));
        String uniqueId = UUID.randomUUID().toString();

        String htmlContent = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; background-color: #efefef; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif;'>" +

                "<!-- Unique identifier to prevent Gmail threading -->" +
                "<div style='display: none; max-height: 0; overflow: hidden;'>" +
                "Password Reset Request - " + formattedDateTime + " - ID: " + uniqueId +
                "</div>" +

                "<table role='presentation' style='width: 100%; border-collapse: collapse; margin: 0; padding: 40px 20px;'>" +
                "<tr><td align='center'>" +

                "<!-- Main Card -->" +
                "<div style='max-width: 600px; background: #ffffff; border-radius: 24px; box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08); overflow: hidden;'>" +

                "<!-- Header with Gradient -->" +
                "<div style='background: linear-gradient(135deg, #48e3cc 0%, #36c5b0 100%); padding: 40px 30px; text-align: center;'>" +
                "<h1 style='margin: 0; font-size: 28px; font-weight: 800; color: #000; letter-spacing: 0.5px;'>" +
                "<span style='color: #000;'>GST</span> <span style='color: #000;'>Medicose</span>" +
                "</h1>" +
                "<p style='margin: 8px 0 0 0; font-size: 16px; color: rgba(0, 0, 0, 0.8); font-weight: 500;'>Management System</p>" +
                "</div>" +

                "<!-- Content -->" +
                "<div style='padding: 40px 30px;'>" +

                "<!-- Greeting -->" +
                "<h2 style='margin: 0 0 20px 0; font-size: 24px; font-weight: 800; color: #000;'>Password Reset Request</h2>" +

                "<p style='margin: 0 0 16px 0; font-size: 15px; color: #333; line-height: 1.6;'>" +
                "Hello," +
                "</p>" +

                "<p style='margin: 0 0 16px 0; font-size: 15px; color: #333; line-height: 1.6;'>" +
                "We received a request to reset your password for your GST Medicose account. Click the button below to create a new password:" +
                "</p>" +

                "<!-- CTA Button -->" +
                "<table role='presentation' style='width: 100%; border-collapse: collapse; margin: 30px 0;'>" +
                "<tr><td align='center'>" +
                "<a href='" + resetLink + "' style='display: inline-block; background: linear-gradient(135deg, #48e3cc 0%, #36c5b0 100%); " +
                "color: #000; padding: 16px 40px; text-decoration: none; border-radius: 12px; font-weight: 700; " +
                "font-size: 16px; box-shadow: 0 4px 15px rgba(72, 227, 204, 0.3); transition: all 0.2s ease;'>" +
                "Reset Password" +
                "</a>" +
                "</td></tr>" +
                "</table>" +

                "<!-- Info Box -->" +
                "<div style='background: #fff5e6; border-left: 4px solid #ff9800; padding: 16px 20px; border-radius: 8px; margin: 25px 0;'>" +
                "<p style='margin: 0; font-size: 14px; color: #666; line-height: 1.5;'>" +
                "<strong style='color: #ff9800;'>⚠️ Important:</strong> This link will expire in <strong>15 minutes</strong> for security reasons." +
                "</p>" +
                "</div>" +

                "<!-- Alternative Link -->" +
                "<p style='margin: 20px 0 0 0; font-size: 13px; color: #777; line-height: 1.6;'>" +
                "If the button doesn't work, copy and paste this link into your browser:" +
                "</p>" +
                "<p style='margin: 8px 0 0 0; font-size: 13px; color: #48e3cc; word-break: break-all; background: #f9f9f9; padding: 12px; border-radius: 6px;'>" +
                resetLink +
                "</p>" +

                "</div>" +

                "<!-- Footer -->" +
                "<div style='background: #f9f9f9; padding: 30px; border-top: 1px solid #eee;'>" +

                "<!-- Divider -->" +
                "<div style='height: 1px; background: linear-gradient(to right, transparent, #eee, transparent); margin: 0 0 20px 0;'></div>" +

                "<!-- Security Notice -->" +
                "<p style='margin: 0 0 12px 0; font-size: 13px; color: #999; line-height: 1.6;'>" +
                "If you didn't request this password reset, please ignore this email. Your password will remain unchanged." +
                "</p>" +

                "<!-- Timestamp -->" +
                "<p style='margin: 0 0 12px 0; font-size: 12px; color: #bbb;'>" +
                "Request sent: " + formattedDateTime +
                "</p>" +

                "<!-- Footer Links -->" +
                "<p style='margin: 0; font-size: 12px; color: #999; text-align: center;'>" +
                "© 2025 GST Medicose Management System. All rights reserved." +
                "</p>" +

                "</div>" +

                "</div>" +

                "</td></tr>" +
                "</table>" +

                "</body>" +
                "</html>";

        emailService.sendEmail(email, "Reset Your Password - GST Medicose", htmlContent);
    }
}
