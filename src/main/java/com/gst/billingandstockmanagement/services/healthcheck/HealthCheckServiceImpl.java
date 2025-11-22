package com.gst.billingandstockmanagement.services.healthcheck;

import com.gst.billingandstockmanagement.repository.HealthCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    private final HealthCheckRepository healthCheckRepository;

    @Autowired
    public HealthCheckServiceImpl(HealthCheckRepository healthCheckRepository) {
        this.healthCheckRepository = healthCheckRepository;
    }

    @Override
    public Map<String, String> checkHealth() {
        Map<String, String> statusMap = new HashMap<>();

        statusMap.put("application", "UP");

        // Run the database check and update the map with the full result
        Map<String, String> dbDetails = checkDatabaseConnection();
        statusMap.putAll(dbDetails); // Add database status AND timestamp to the map

        // Determine overall status based on the specific database status key
        if ("UP".equals(statusMap.get("database_status"))) {
            statusMap.put("status", "UP");
        } else {
            statusMap.put("status", "DEGRADED");
        }

        return statusMap;
    }

    private Map<String, String> checkDatabaseConnection() {
        Map<String, String> dbMap = new HashMap<>();
        try {
            // Get the timestamp from the database
            Object dbTime = healthCheckRepository.getCurrentDbTime();

            // Add the success status and the actual timestamp value
            dbMap.put("database_status", "UP");
            dbMap.put("database_timestamp", dbTime.toString());

            // Add the local time the check was performed for context
            dbMap.put("check_time", LocalDateTime.now().toString());

            return dbMap;
        } catch (Exception e) {
            System.err.println("Database check failed: " + e.getMessage());

            // Add failure status and error details
            dbMap.put("database_status", "DOWN");
            dbMap.put("database_error", e.getMessage());

            return dbMap;
        }
    }
}