package com.gst.billingandstockmanagement.controllers;

import java.io.IOException;
import java.util.Map;

import com.gst.billingandstockmanagement.dto.ForgotPasswordRequestDTO;
import com.gst.billingandstockmanagement.dto.ResetPasswordDTO;
import com.gst.billingandstockmanagement.services.resetpassword.PasswordResetService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gst.billingandstockmanagement.dto.AuthenticationRequest;
import com.gst.billingandstockmanagement.entities.User;
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

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    @PostMapping("/authenticate")
    public void createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest, HttpServletResponse response) throws BadCredentialsException, DisabledException, UsernameNotFoundException, IOException, JSONException, ServletException {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Incorrect username or password.");
        } catch (DisabledException disabledException) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User is not activated");
            return;
        }
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        User user = userRepository.findFirstByEmail(userDetails.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails.getUsername());

        response.getWriter().write(new JSONObject()
                .put("userId", user.getId())
                .put("role", user.getUserRole())
                .toString()
        );
        response.addHeader("Access-Control-Expose-Headers", "Authorization");
        response.addHeader("Access-Control-Allow-Headers", "Authorization, X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept, X-Custom-header");
        response.addHeader(HEADER_STRING, TOKEN_PREFIX + jwt);
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
}

