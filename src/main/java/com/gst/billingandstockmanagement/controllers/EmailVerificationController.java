package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.services.email.EmailService;
import com.gst.billingandstockmanagement.services.user.UserService;
import com.gst.billingandstockmanagement.utils.EmailTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class EmailVerificationController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Value("${app.client.url}")
    private String frontendUrl;

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token is required"));
        }

        boolean verified = userService.verifyEmail(token);
        if (!verified) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid or expired verification token"));
        }

        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        String token = userService.createVerificationToken(email);
        if (token != null) {
            com.gst.billingandstockmanagement.entities.User user =
                    userService.getUserByEmail(email);
            String firstname = user != null ? user.getFirstname() : "there";
            String verifyLink = frontendUrl + "/verify-email?token=" + token;
            emailService.sendEmail(
                    email,
                    "Verify your GST Medicose account",
                    EmailTemplates.verificationEmail(firstname, verifyLink)
            );
        }

        return ResponseEntity.ok(Map.of("message", "If this email exists and is unverified, a new link has been sent"));
    }
}