package com.gst.billingandstockmanagement.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.security.CustomUserDetails;
import com.gst.billingandstockmanagement.services.user.UserService;
import com.gst.billingandstockmanagement.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
public class GoogleAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${google.client-id}")
    private String googleClientId;

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    @PostMapping("/auth/google")
    public void googleSignIn(@RequestBody Map<String, String> body, HttpServletResponse response) throws Exception {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "idToken is required");
            return;
        }

        // Verify the Google ID token — cryptographic check, no network call after first key fetch
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken googleIdToken;
        try {
            googleIdToken = verifier.verify(idToken);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Google token");
            return;
        }

        if (googleIdToken == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Google token");
            return;
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        String email = payload.getEmail();
        String firstname = (String) payload.get("given_name");
        String lastname = (String) payload.get("family_name");
        String googleId = payload.getSubject();

        // Find or create user — silently links if email already exists
        User user = userService.findOrCreateGoogleUser(email, firstname, lastname, googleId);

        // Issue JWT exactly like regular login
        String jwt = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getUserRole() != null ? user.getUserRole().toString() : "USER"
        );

        response.getWriter().write(new JSONObject()
                .put("message", "Login successful")
                .toString()
        );
        response.setContentType("application/json");
        response.addHeader("Access-Control-Expose-Headers", "Authorization");
        response.addHeader("Access-Control-Allow-Headers", "Authorization, X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept, X-Custom-header");
        response.addHeader(HEADER_STRING, TOKEN_PREFIX + jwt);
    }
}