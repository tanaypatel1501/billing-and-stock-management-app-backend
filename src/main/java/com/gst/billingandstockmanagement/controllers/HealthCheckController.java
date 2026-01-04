package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.services.healthcheck.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health-check")
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    @Autowired
    public HealthCheckController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/server")
    public ResponseEntity<Map<String, Object>> appServerHealth() {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .body(Map.of(
                        "status", "UP",
                        "application", "BillingAndStockManagement",
                        "check_time", LocalDateTime.now().toString()
                ));
    }

    @GetMapping("/db-server")
    public ResponseEntity<Map<String, String>> checkServerAndDBHealth() {
        Map<String, String> healthStatus = healthCheckService.checkHealth();

        ResponseEntity.BodyBuilder responseBuilder =
                "UP".equals(healthStatus.get("status"))
                        ? ResponseEntity.ok()
                        : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE);

        return responseBuilder
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .body(healthStatus);
    }
}