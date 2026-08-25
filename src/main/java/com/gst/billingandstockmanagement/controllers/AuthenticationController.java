package com.gst.billingandstockmanagement.controllers;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

import com.gst.billingandstockmanagement.dto.ForgotPasswordRequestDTO;
import com.gst.billingandstockmanagement.dto.ResetPasswordDTO;
import com.gst.billingandstockmanagement.entities.MobileSessionToken;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.MobileSessionTokenRepository;
import com.gst.billingandstockmanagement.services.resetpassword.PasswordResetService;
import com.gst.billingandstockmanagement.security.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.gst.billingandstockmanagement.dto.AuthenticationRequest;
import com.gst.billingandstockmanagement.repository.UserRepository;
import com.gst.billingandstockmanagement.services.user.UserService;
import com.gst.billingandstockmanagement.utils.JwtUtil;

@RestController
public class AuthenticationController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private MobileSessionTokenRepository mobileSessionTokenRepository;

    @Value("${mobile.session.expirationMs}")
    private long mobileSessionExpirationMs;

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    private static final String MOBILE_CLIENT_TYPE = "MOBILE_APP";

    private final SecureRandom secureRandom = new SecureRandom();

    @PostMapping("/authenticate")
    public void createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest, HttpServletResponse response) throws BadCredentialsException, DisabledException, UsernameNotFoundException, IOException, JSONException, ServletException {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Incorrect username or password.");
        } catch (DisabledException disabledException) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User is not activated");
            return;
        }

        final CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        if (!userDetails.isEmailVerified()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(new JSONObject()
                    .put("error", "EMAIL_NOT_VERIFIED")
                    .put("message", "Please verify your email before logging in.")
                    .toString());
            return;
        }

        final String jwt = jwtUtil.generateToken(userDetails.getUsername(), userDetails.getId(), userDetails.getRole());

        JSONObject responseBody = new JSONObject().put("message", "Login successful");

        // Only mobile app logins get a long-lived session token — web logins
        // are completely unaffected and receive nothing extra here.
        if (MOBILE_CLIENT_TYPE.equals(authenticationRequest.getClientType())) {
            String mobileSessionToken = generateMobileSessionToken(userDetails.getId());
            responseBody.put("mobileSessionToken", mobileSessionToken);
        }

        response.getWriter().write(responseBody.toString());
        response.addHeader("Access-Control-Expose-Headers", "Authorization");
        response.addHeader("Access-Control-Allow-Headers", "Authorization, X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept, X-Custom-header");
        response.addHeader(HEADER_STRING, TOKEN_PREFIX + jwt);
    }

    private String generateMobileSessionToken(Long userId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for id: " + userId));

        MobileSessionToken sessionToken = new MobileSessionToken();
        sessionToken.setToken(token);
        sessionToken.setUser(user);
        sessionToken.setExpiryDate(LocalDateTime.now().plusNanos(mobileSessionExpirationMs * 1_000_000));
        sessionToken.setCreatedAt(LocalDateTime.now());
        sessionToken.setRevoked(false);

        mobileSessionTokenRepository.save(sessionToken);

        return token;
    }
    
    @PostMapping("/refresh-token")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Extract the token from the request header
        String authHeader = request.getHeader(HEADER_STRING);
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            token = authHeader.substring(TOKEN_PREFIX.length());
            username = jwtUtil.extractUsername(token);
        }

        // Check if the token can be refreshed
        if (username != null && jwtUtil.canTokenBeRefreshed(token)) {
            // Refresh the token
            String refreshedToken = jwtUtil.refreshToken(token);
            response.addHeader("Access-Control-Expose-Headers", "Authorization");
            response.addHeader("Access-Control-Allow-Headers", "Authorization, X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept, X-Custom-header");
            response.addHeader(HEADER_STRING, TOKEN_PREFIX + refreshedToken);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "The token cannot be refreshed.");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO request) {

        passwordResetService.createAndSendResetToken(request.getEmail());

        // Always return success (security best practice)
        return ResponseEntity.ok(Map.of("message","If the email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordDTO request) {

        boolean success = passwordResetService
                .resetPassword(request.getToken(), request.getNewPassword());

        if (!success) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid or expired reset token"));
        }

        return ResponseEntity.ok(
                Map.of("message", "Password reset successful")
        );
    }

    @Transactional
    @PostMapping("/logout")
    public void logout(@RequestBody(required = false) LogoutRequest request, HttpServletResponse response) throws IOException, JSONException {
        if (request != null && request.getMobileSessionToken() != null) {
            mobileSessionTokenRepository.revokeByToken(request.getMobileSessionToken());
        }

        response.getWriter().write(new JSONObject()
                .put("message", "Logged out")
                .toString());
    }

    public static class LogoutRequest {
        private String mobileSessionToken;

        public String getMobileSessionToken() {
            return mobileSessionToken;
        }

        public void setMobileSessionToken(String mobileSessionToken) {
            this.mobileSessionToken = mobileSessionToken;
        }
    }
}

