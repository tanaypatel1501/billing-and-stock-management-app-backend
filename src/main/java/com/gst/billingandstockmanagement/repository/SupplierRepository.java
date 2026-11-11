package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Supplier;
import com.gst.billingandstockmanagement.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByUserAndNameContainingIgnoreCase(User user, String name);
    Page<Supplier> findByUserAndNameContainingIgnoreCase(User user, String name, Pageable pageable);
    Page<Supplier> findByUser(User user, Pageable pageable);
}