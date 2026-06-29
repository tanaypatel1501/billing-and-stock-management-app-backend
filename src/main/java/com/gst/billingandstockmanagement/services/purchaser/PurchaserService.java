package com.gst.billingandstockmanagement.services.purchaser;

import com.gst.billingandstockmanagement.dto.PurchaserDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaserService {
    PurchaserDTO savePurchaser(PurchaserDTO dto);
    Page<PurchaserDTO> getPagedByUser(Long userId, String search, Pageable pageable);
    List<PurchaserDTO> searchByName(Long userId, String name);
    void deletePurchaser(Long purchaserId, Long requestingUserId);
}