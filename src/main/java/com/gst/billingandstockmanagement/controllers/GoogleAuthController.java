package com.gst.billingandstockmanagement.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.gst.billingandstockmanagement.entities.MobileSessionToken;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.MobileSessionTokenRepository;
import com.gst.billingandstockmanagement.services.user.UserService;
import com.gst.billingandstockmanagement.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@RestController
public class GoogleAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MobileSessionTokenRepository mobileSessionTokenRepository;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${mobile.session.expirationMs}")
    private long mobileSessionExpirationMs;

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    private static final String MOBILE_CLIENT_TYPE = "MOBILE_APP";

    private final SecureRandom secureRandom = new SecureRandom();

    @PostMapping("/auth/google")
    public void googleSignIn(@RequestBody Map<String, String> body, HttpServletResponse response) throws Exception {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "idToken is required");
            return;
        }

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

        User user = userService.findOrCreateGoogleUser(email, firstname, lastname, googleId);

        String jwt = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getUserRole() != null ? user.getUserRole().toString() : "USER"
        );

        JSONObject responseBody = new JSONObject().put("message", "Login successful");

        if (MOBILE_CLIENT_TYPE.equals(body.get("clientType"))) {
            String mobileSessionToken = generateMobileSessionToken(user.getId());
            responseBody.put("mobileSessionToken", mobileSessionToken);
        }

        response.getWriter().write(responseBody.toString());
        response.setContentType("application/json");
        response.addHeader("Access-Control-Expose-Headers", "Authorization");
        response.addHeader("Access-Control-Allow-Headers", "Authorization, X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept, X-Custom-header");
        response.addHeader(HEADER_STRING, TOKEN_PREFIX + jwt);
    }

    private String generateMobileSessionToken(Long userId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        MobileSessionToken sessionToken = new MobileSessionToken();
        sessionToken.setToken(token);
        sessionToken.setUser(userService.getUserById(userId));
        sessionToken.setExpiryDate(LocalDateTime.now().plusNanos(mobileSessionExpirationMs * 1_000_000));
        sessionToken.setCreatedAt(LocalDateTime.now());
        sessionToken.setRevoked(false);

        mobileSessionTokenRepository.save(sessionToken);
        return token;
    }
}