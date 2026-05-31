package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.ProductRequest;
import com.gst.billingandstockmanagement.enums.ProductRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRequestRepository extends JpaRepository<ProductRequest, Long> {

    // All requests submitted by a specific user
    List<ProductRequest> findByRequestedByIdOrderByCreatedAtDesc(Long userId);

    // All requests with a given status (used by admin + auto-approver)
    List<ProductRequest> findByStatusOrderByCreatedAtAsc(ProductRequestStatus status);
}