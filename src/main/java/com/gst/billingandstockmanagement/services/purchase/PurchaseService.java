package com.gst.billingandstockmanagement.services.purchase;

import com.gst.billingandstockmanagement.dto.PurchaseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PurchaseService {
    PurchaseDTO submitPurchase(PurchaseDTO dto);
    PurchaseDTO getPurchaseById(Long purchaseId, Long userId);
    Page<PurchaseDTO> getPurchases(Long userId, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
    void revertPurchase(Long purchaseId, Long userId);
}