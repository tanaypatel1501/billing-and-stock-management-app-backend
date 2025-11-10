package com.gst.billingandstockmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gst.billingandstockmanagement.entities.Details;

@Repository
public interface DetailsRepository extends JpaRepository<Details, Long> {
    // You can add custom query methods here if needed
}
