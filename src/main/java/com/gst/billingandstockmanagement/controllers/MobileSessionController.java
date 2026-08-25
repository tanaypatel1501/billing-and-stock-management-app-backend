package com.gst.billingandstockmanagement.controllers;

import java.io.IOException;
import java.time.LocalDateTime;

import com.gst.billingandstockmanagement.entities.MobileSessionToken;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.MobileSessionTokenRepository;
import com.gst.billingandstockmanagement.utils.JwtUtil;

import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MobileSessionController {

    @Autowired
    private MobileSessionTokenRepository mobileSessionTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    @PostMapping("/mobile-session/renew")
    public void renew(@RequestBody MobileSessionRenewRequest request, HttpServletResponse response) throws IOException, JSONException {

        MobileSessionToken sessionToken = mobileSessionTokenRepository
                .findByToken(request.getMobileSessionToken())
                .orElse(null);

        if (sessionToken == null
                || sessionToken.isRevoked()
                || sessionToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or expired mobile session.");
            return;
        }

        User user = sessionToken.getUser();
        final String jwt = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getUserRole().toString());

        response.getWriter().write(new JSONObject()
                .put("message", "Session renewed")
                .toString());
        response.addHeader("Access-Control-Expose-Headers", "Authorization");
        response.addHeader("Access-Control-Allow-Headers", "Authorization, X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept, X-Custom-header");
        response.addHeader(HEADER_STRING, TOKEN_PREFIX + jwt);
    }

    public static class MobileSessionRenewRequest {
        private String mobileSessionToken;

        public String getMobileSessionToken() {
            return mobileSessionToken;
        }

        public void setMobileSessionToken(String mobileSessionToken) {
            this.mobileSessionToken = mobileSessionToken;
        }
    }
}