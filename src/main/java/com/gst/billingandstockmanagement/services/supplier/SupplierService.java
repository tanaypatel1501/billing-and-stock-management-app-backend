package com.gst.billingandstockmanagement.services.supplier;

import com.gst.billingandstockmanagement.dto.SupplierDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierService {
    SupplierDTO saveSupplier(SupplierDTO dto);
    Page<SupplierDTO> getPagedByUser(Long userId, String search, Pageable pageable);
    List<SupplierDTO> searchByName(Long userId, String name);
    void deleteSupplier(Long supplierId, Long requestingUserId);
}