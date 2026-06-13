package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Purchaser;
import com.gst.billingandstockmanagement.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface PurchaserRepository extends JpaRepository<Purchaser, Long> {
    List<Purchaser> findByUserAndNameContainingIgnoreCase(User user, String name);
    Page<Purchaser> findByUserAndNameContainingIgnoreCase(User user, String name, Pageable pageable);
    Page<Purchaser> findByUser(User user, Pageable pageable);
}