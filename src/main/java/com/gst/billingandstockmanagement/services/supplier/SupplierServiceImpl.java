package com.gst.billingandstockmanagement.services.supplier;

import com.gst.billingandstockmanagement.dto.SupplierDTO;
import com.gst.billingandstockmanagement.entities.Supplier;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.PurchaseRepository;
import com.gst.billingandstockmanagement.repository.SupplierRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public SupplierDTO saveSupplier(SupplierDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier name is required");
        }
        String name = dto.getName().trim();

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Supplier supplier;

        if (dto.getId() != null) {
            supplier = supplierRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Supplier not found"));
            if (!supplier.getUser().getId().equals(dto.getUserId())) {
                throw new AccessDeniedException("You do not have access to this supplier.");
            }
        } else {
            // Same behaviour as purchasers: reuse an existing supplier with the same name
            List<Supplier> existing = supplierRepository
                    .findByUserAndNameContainingIgnoreCase(user, name)
                    .stream()
                    .filter(s -> s.getName().equalsIgnoreCase(name))
                    .collect(Collectors.toList());

            supplier = existing.isEmpty() ? new Supplier() : existing.get(0);
        }

        supplier.setUser(user);
        supplier.setName(name);
        supplier.setDl1(dto.getDl1());
        supplier.setDl2(dto.getDl2());
        supplier.setGstin(dto.getGstin());
        supplier.setState(dto.getState());
        supplier.setPhoneNumber(dto.getPhoneNumber());

        return toDTO(supplierRepository.save(supplier));
    }

    @Override
    public Page<SupplierDTO> getPagedByUser(Long userId, String search, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Supplier> page = (search != null && !search.isBlank())
                ? supplierRepository.findByUserAndNameContainingIgnoreCase(user, search, pageable)
                : supplierRepository.findByUser(user, pageable);

        return page.map(this::toDTO);
    }

    @Override
    public List<SupplierDTO> searchByName(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return supplierRepository
                .findByUserAndNameContainingIgnoreCase(user, name)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteSupplier(Long supplierId, Long requestingUserId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        if (!supplier.getUser().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("You do not have access to this supplier.");
        }
        // Purchases keep their snapshot of the supplier name and GSTIN
        purchaseRepository.nullifySupplierReference(supplierId);
        supplierRepository.deleteById(supplierId);
    }

    private SupplierDTO toDTO(Supplier s) {
        SupplierDTO dto = new SupplierDTO();
        dto.setId(s.getId());
        dto.setUserId(s.getUser().getId());
        dto.setName(s.getName());
        dto.setDl1(s.getDl1());
        dto.setDl2(s.getDl2());
        dto.setGstin(s.getGstin());
        dto.setState(s.getState());
        dto.setPhoneNumber(s.getPhoneNumber());
        return dto;
    }
}