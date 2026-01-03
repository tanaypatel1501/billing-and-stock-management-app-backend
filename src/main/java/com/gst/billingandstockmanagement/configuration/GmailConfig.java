package com.gst.billingandstockmanagement.configuration;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class GmailConfig {

    @Value("${gmail.client-id}")
    private String clientId;

    @Value("${gmail.client-secret}")
    private String clientSecret;

    @Value("${gmail.refresh-token}")
    private String refreshToken;

    @Bean
    public Gmail gmailService() throws Exception {

        GoogleCredential credential = new GoogleCredential.Builder()
                .setClientSecrets(clientId, clientSecret) // <-- client_secret must be passed here
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .build()
                .setRefreshToken(refreshToken);

        credential.refreshToken();

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        )
                .setApplicationName("GST Medicose")
                .build();
    }
}
