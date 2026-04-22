package com.gst.billingandstockmanagement.services.purchaser;

import com.gst.billingandstockmanagement.dto.PurchaserDTO;
import java.util.List;

public interface PurchaserService {
    PurchaserDTO savePurchaser(PurchaserDTO dto);
    List<PurchaserDTO> searchByName(Long userId, String name);
    void deletePurchaser(Long purchaserId);
}