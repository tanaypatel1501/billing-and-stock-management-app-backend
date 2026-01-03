package com.gst.billingandstockmanagement.services.resetpassword;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private JavaMailSender mailSender;

    @Value("${app.client.reset-password-url}")
    private String resetPasswordUrl;

    @Async
    @Override
    public void createAndSendResetToken(String email) {
        User user = userRepository.findFirstByEmail(email);

        // Security: do not leak email existence
        if (user == null) {
            return;
        }

        // Remove old tokens
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Reset Your Password");
        message.setText(
                "Hello,\n\n" +
                        "Click the link below to reset your password:\n\n" +
                        resetLink +
                        "\n\nThis link will expire in 15 minutes.\n\n" +
                        "If you did not request this, please ignore this email."
        );

        mailSender.send(message);
    }
}
