package com.gst.billingandstockmanagement.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class SelfPingScheduler {

    private static final Logger log = LoggerFactory.getLogger(SelfPingScheduler.class);

    @Value("${SELF_URL:}")
    private String selfUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Ping self every 4 minutes to prevent sleeping
    @Scheduled(fixedDelay = 240000)
    public void selfPing() {
        if (selfUrl == null || selfUrl.isEmpty()) return;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(selfUrl + "/health-check/server"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("Self ping status: {}", response.statusCode());
        } catch (Exception e) {
            log.debug("Self ping failed: {}", e.getMessage());
        }
    }
}