package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.PurchaseItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseItemsRepository extends JpaRepository<PurchaseItems, Long> {
    List<PurchaseItems> findByPurchaseId(Long purchaseId);
}