package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.services.healthcheck.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health-check")
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    @Autowired
    public HealthCheckController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> checkHealth() {
        Map<String, String> healthStatus = healthCheckService.checkHealth();

        // Determine HTTP status based on the service response
        if ("UP".equals(healthStatus.get("status"))) {
            return ResponseEntity.ok(healthStatus);
        } else {
            // Use 503 Service Unavailable or 500 Internal Server Error for degraded status
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthStatus);
        }
    }
}