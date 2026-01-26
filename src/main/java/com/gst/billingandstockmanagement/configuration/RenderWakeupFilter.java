package com.gst.billingandstockmanagement.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class RenderWakeupFilter implements Filter {

    // Pull the URL from the environment variable we just created
    @Value("${RENDER_BACKUP_URL:}")
    private String renderBackupUrl;

    @Value("${IS_PRIMARY_SERVER:false}")
    private boolean isPrimaryServer;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Process the request immediately so the user isn't delayed
        chain.doFilter(request, response);

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Check if URL is configured and if the path is an API call
        if (isPrimaryServer &&
                renderBackupUrl != null && !renderBackupUrl.isEmpty() &&
                !path.contains("health-check") &&
                (path.contains("/api/") || path.contains("/authenticate"))) {

            executorService.submit(() -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(renderBackupUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
                    connection.connect();
                    connection.getResponseCode();
                } catch (Exception e) {
                    // Fail silently
                }
            });
        }
    }
}