package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Purchaser;
import com.gst.billingandstockmanagement.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaserRepository extends JpaRepository<Purchaser, Long>, JpaSpecificationExecutor<Purchaser> {
    List<Purchaser> findByUser(User user);
    List<Purchaser> findByUserAndNameContainingIgnoreCase(User user, String name);
}