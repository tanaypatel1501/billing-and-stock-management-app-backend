package com.gst.billingandstockmanagement.configuration;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class RenderWarmupService {

    private static final Logger log = LoggerFactory.getLogger(RenderWarmupService.class);

    @Value("${RENDER_BACKUP_URL:}")
    private String renderBackupUrl;

    public enum WarmState { COLD, WARMING, WARM }

    private volatile WarmState state = WarmState.COLD;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public WarmState getState() {
        return state;
    }

    public synchronized void triggerWarmup() {
        if (state != WarmState.COLD) return; // already warming or warm
        if (renderBackupUrl == null || renderBackupUrl.isEmpty()) return; // not configured

        state = WarmState.WARMING;
        log.info("Triggering Render warmup...");

        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 30; i++) { // poll every 10s for max 5 mins
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(renderBackupUrl + "/health-check/server"))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient
                            .send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        state = WarmState.WARM;
                        log.info("Render is warm and ready.");
                        return;
                    }
                } catch (Exception ignored) {}

                try { Thread.sleep(10_000); } catch (InterruptedException ignored) {}
            }

            // Timed out — reset so it retries on next startup
            state = WarmState.COLD;
            log.warn("Render warmup timed out, resetting to COLD.");
        });
    }

    @PreDestroy
    public void onShutdown() {
        state = WarmState.COLD;
    }
}