package com.gst.billingandstockmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthCheckRepository extends JpaRepository<Object, Long> {

    @Query(value = "SELECT CURRENT_TIMESTAMP()", nativeQuery = true) // Safer query for most SQL databases
    Object getCurrentDbTime();
}