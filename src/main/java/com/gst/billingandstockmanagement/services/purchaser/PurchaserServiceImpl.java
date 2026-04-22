package com.gst.billingandstockmanagement.services.purchaser;

import com.gst.billingandstockmanagement.dto.PurchaserDTO;
import com.gst.billingandstockmanagement.entities.Purchaser;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.PurchaserRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaserServiceImpl implements PurchaserService {

    @Autowired
    private PurchaserRepository purchaserRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public PurchaserDTO savePurchaser(PurchaserDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Purchaser purchaser;

        if (dto.getId() != null) {
            purchaser = purchaserRepository.findById(dto.getId())
                    .orElse(new Purchaser());
        } else {
            List<Purchaser> existing = purchaserRepository
                    .findByUserAndNameContainingIgnoreCase(user, dto.getName())
                    .stream()
                    .filter(p -> p.getName().equalsIgnoreCase(dto.getName()))
                    .collect(Collectors.toList());

            purchaser = existing.isEmpty() ? new Purchaser() : existing.get(0);
        }

        purchaser.setUser(user);
        purchaser.setName(dto.getName());
        purchaser.setDl1(dto.getDl1());
        purchaser.setDl2(dto.getDl2());
        purchaser.setGstin(dto.getGstin());

        Purchaser saved = purchaserRepository.save(purchaser);
        return toDTO(saved);
    }

    @Override
    public List<PurchaserDTO> searchByName(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return purchaserRepository
                .findByUserAndNameContainingIgnoreCase(user, name)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deletePurchaser(Long purchaserId) {
        purchaserRepository.deleteById(purchaserId);
    }

    private PurchaserDTO toDTO(Purchaser p) {
        PurchaserDTO dto = new PurchaserDTO();
        dto.setId(p.getId());
        dto.setUserId(p.getUser().getId());
        dto.setName(p.getName());
        dto.setDl1(p.getDl1());
        dto.setDl2(p.getDl2());
        dto.setGstin(p.getGstin());
        return dto;
    }
}