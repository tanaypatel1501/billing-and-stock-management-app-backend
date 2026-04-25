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
public class KoyebWarmupService {

    private static final Logger log = LoggerFactory.getLogger(KoyebWarmupService.class);

    @Value("${KOYEB_BACKUP_URL:}")
    private String koyebBackupUrl;

    public enum WarmState { COLD, WARMING, WARM }

    private volatile WarmState state = WarmState.COLD;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public WarmState getState() {
        return state;
    }

    public synchronized void triggerWarmup() {
        if (state != WarmState.COLD) return;
        if (koyebBackupUrl == null || koyebBackupUrl.isEmpty()) return;

        state = WarmState.WARMING;
        log.info("Triggering Koyeb warmup...");

        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 30; i++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(koyebBackupUrl + "/health-check/server"))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient
                            .send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        state = WarmState.WARM;
                        log.info("Koyeb is warm and ready.");
                        return;
                    }
                } catch (Exception ignored) {}

                try { Thread.sleep(10_000); } catch (InterruptedException ignored) {}
            }

            state = WarmState.COLD;
            log.warn("Koyeb warmup timed out, resetting to COLD.");
        });
    }

    @PreDestroy
    public void onShutdown() {
        state = WarmState.COLD;
    }
}