package com.gst.billingandstockmanagement.services.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate; // Re-import JdbcTemplate
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    // Dependency to check database connectivity
    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    @Autowired
    // Inject JdbcTemplate instead of HealthCheckRepository
    public HealthCheckServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> checkHealth() {
        Map<String, String> statusMap = new HashMap<>();

        // Check 1: Application Status (UP if bean initialized)
        statusMap.put("application", "UP");

        // Check 2: Database Connectivity (Most Important)
        Map<String, String> dbDetails = checkDatabaseConnection();
        statusMap.putAll(dbDetails); // Add database status AND timestamp/error to the map

        // Determine overall status based on the specific database status key
        if ("UP".equals(statusMap.get("database_status"))) {
            statusMap.put("status", "UP");
        } else {
            statusMap.put("status", "DOWN");
        }

        return statusMap;
    }

    private Map<String, String> checkDatabaseConnection() {
        Map<String, String> dbMap = new HashMap<>();
        try {
            // Execute the non-destructive query to get the current timestamp
            // Using String.class for flexibility, as different DBs return different timestamp types.
            String dbTime = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP()", String.class);

            // Add the success status and the actual timestamp value
            dbMap.put("database_status", "UP");
            dbMap.put("database_timestamp", dbTime);

            // Add the local time the check was performed for context
            dbMap.put("check_time", LocalDateTime.now().toString());

            return dbMap;
        } catch (Exception e) {
            // Log the exception for debugging purposes
            log.error("Database health check failed", e);

            // Add failure status and error details
            dbMap.put("database_status", "DOWN");
            dbMap.put("database_error", e.getMessage());

            return dbMap;
        }
    }
}