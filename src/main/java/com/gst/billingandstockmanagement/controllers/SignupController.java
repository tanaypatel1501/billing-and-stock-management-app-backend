package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.services.email.EmailService;
import com.gst.billingandstockmanagement.utils.EmailTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gst.billingandstockmanagement.dto.SignupDTO;
import com.gst.billingandstockmanagement.dto.UserDTO;
import com.gst.billingandstockmanagement.services.user.UserService;

import java.util.Map;

@RestController
public class SignupController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Value("${app.client.url}")
    private String frontendUrl;

    @PostMapping("/sign-up")
    public ResponseEntity<?> signupUser(@RequestBody SignupDTO signupDTO) {
        if (userService.hasUserWithEmail(signupDTO.getEmail())) {
            return new ResponseEntity<>("User already exists", HttpStatus.NOT_ACCEPTABLE);
        }

        UserDTO createdUser = userService.createUser(signupDTO);
        if (createdUser == null) {
            return new ResponseEntity<>("User not created. Come again later!", HttpStatus.BAD_REQUEST);
        }

        // Generate verification token and send email
        String token = userService.createVerificationToken(signupDTO.getEmail());
        if (token != null) {
            String verifyLink = frontendUrl + "/verify-email?token=" + token;
            emailService.sendEmail(
                    signupDTO.getEmail(),
                    "Verify your GST Medicose account",
                    EmailTemplates.verificationEmail(signupDTO.getFirstname(), verifyLink)
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful! Please check your email to verify your account."));
    }
}