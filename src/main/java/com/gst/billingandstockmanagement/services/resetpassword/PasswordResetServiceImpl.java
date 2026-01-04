package com.gst.billingandstockmanagement.services.resetpassword;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.gst.billingandstockmanagement.services.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm:ss a");
        String formattedDateTime = now.format(formatter);
        String uniqueId = UUID.randomUUID().toString();

        String htmlContent =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "</head>" +

                        "<body style='margin:0; padding:0; background-color:#efefef; " +
                        "font-family:-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Arial, sans-serif;'>" +

                        /* Hidden text to prevent Gmail threading */
                        "<div style='display:none; max-height:0; overflow:hidden;'>" +
                        "Reset your GST Medicose password. Security ID: " + uniqueId +
                        "</div>" +

                        "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' " +
                        "style='background-color:#f4f7f6;'>" +
                        "<tr>" +
                        "<td align='center' style='padding:40px 10px;'>" +

                        "<div style='max-width:600px; width:100%; background:#ffffff; " +
                        "border-radius:16px; box-shadow:0 4px 20px rgba(0,0,0,0.05); " +
                        "border:1px solid #e1e8e5; overflow:hidden;'>" +

                        /* ===== Header ===== */
                        "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">" +
                        "<tr>" +
                        "<td bgcolor=\"#121212\" align=\"center\" style=\"padding:45px 20px;\">" +
                        "<h1 style=\"margin:0; font-size:32px; font-weight:800; color:#ffffff;\">" +
                        "GST <span style=\"color:#48e3cc;\">Medicose</span>" +
                        "</h1>" +
                        "<p style=\"margin:10px 0 0; font-size:12px; color:#cccccc;\">" +
                        "Management System" +
                        "</p>" +
                        "</td>" +
                        "</tr>" +
                        "</table>" +

                        /* ===== Content ===== */
                        "<div style='padding:40px 30px;'>" +

                        "<h2 style='margin:0 0 20px; font-size:24px; font-weight:800; color:#000;'>" +
                        "Password Reset Request</h2>" +

                        "<p style='margin:0 0 16px; font-size:15px; color:#333; line-height:1.6;'>Hello,</p>" +

                        "<p style='margin:0 0 16px; font-size:15px; color:#333; line-height:1.6;'>" +
                        "We received a request to reset your password for your GST Medicose account. " +
                        "Click the button below to create a new password:" +
                        "</p>" +

                        /* ===== CTA Button ===== */
                        "<table role='presentation' width='100%' style='margin:30px 0;'>" +
                        "<tr><td align='center'>" +
                        "<a href='" + resetLink + "' " +
                        "style='display:inline-block; background:linear-gradient(135deg,#48e3cc 0%,#36c5b0 100%);" +
                        "color:#000; padding:16px 40px; text-decoration:none; border-radius:12px; " +
                        "font-weight:700; font-size:16px; box-shadow:0 4px 15px rgba(72,227,204,0.3);'>" +
                        "Reset Password</a>" +
                        "</td></tr>" +
                        "</table>" +

                        /* ===== Expiry Warning ===== */
                        "<table role='presentation' width='100%' " +
                        "style='background-color:#fff9f0; border-radius:10px; border-left:4px solid #ffe8cc;'>" +
                        "<tr><td style='padding:15px 20px;'>" +
                        "<p style='margin:0; font-size:14px; color:#666; line-height:1.5;'>" +
                        "<strong style='color:#856404; display:inline-flex; align-items:center;'>" +
                        "<span style='margin-right:6px;'>⚠️</span>Important:</strong> " +
                        "This link will expire in <strong>15 minutes</strong> for security reasons." +
                        "</p>" +
                        "</td></tr>" +
                        "</table>" +

                        /* ===== Fallback Link ===== */
                        "<div style='margin-top:40px; padding-top:30px; border-top:1px solid #eee;'>" +

                        "<p style='margin:0 0 10px; font-size:12px; color:#999;'>" +
                        "If the button doesn't work, copy and paste this link into your browser:" +
                        "</p>" +

                        "<p style='" +
                        "margin:8px 0 0; font-size:13px; color:#48e3cc; background:#f9f9f9; " +
                        "padding:12px; border-radius:6px; white-space:nowrap; overflow-x:auto;'>" +
                        resetLink +
                        "</p>" +

                        "<p style='margin:10px 0 0; font-size:13px; color:#999; line-height:1.6;'>" +
                        "If you didn't request this password reset, please ignore this email. " +
                        "Your password will remain unchanged." +
                        "</p>" +
                        "</div>" +
                        "</div>" +

                        /* ===== Footer ===== */
                        "<div style='background:#fafafa; padding:40px; text-align:center; border-top:1px solid #f0f0f0;'>" +
                        "<p style='margin:0 0 10px; font-size:13px; color:#666;'>" +
                        "Need help? Contact us at " +
                        "<a href='mailto:gstmedicose+support@gmail.com' " +
                        "style='color:#48e3cc; text-decoration:none; font-weight:600'>gstmedicose.support@gmail.com</a>" +
                        "</p>" +

                        "<p style='margin:0; font-size:12px; color:#bbb;'>" +
                        "Request sent on: " + formattedDateTime + " | ID: " + uniqueId.substring(0, 8) + "<br>" +
                        "&copy; 2026 GST Medicose. All Rights Reserved." +
                        "</p>" +
                        "</div>" +

                        "</div>" +
                        "</td>" +
                        "</tr>" +
                        "</table>" +
                        "</body>" +
                        "</html>";

        emailService.sendEmail(email, "Reset Your Password - GST Medicose", htmlContent);
    }
}
