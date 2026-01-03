package com.gst.billingandstockmanagement.services.details;

import com.gst.billingandstockmanagement.utils.LogoStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.gst.billingandstockmanagement.dto.DetailsDTO;
import com.gst.billingandstockmanagement.entities.Details;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.DetailsRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DetailsServiceImpl implements DetailsService {

    @Autowired
    private DetailsRepository detailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LogoStorageService logoStorageService;


    @Value("${branding.default-logo-url}")
    private String defaultLogoUrl;

    @Override
    public DetailsDTO createDetails(Long userId, DetailsDTO detailsDTO, MultipartFile logo) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Details details = new Details();
        details.setUser(user);

        mapDetailsDTOToDetails(detailsDTO, details);

        String logoUrl = logoStorageService.uploadLogo(logo, userId);
        details.setLogoUrl(logoUrl);

        Details savedDetails = detailsRepository.save(details);
        return mapDetailsToDetailsDTO(savedDetails);
    }

    @Override
    public DetailsDTO updateDetails(Long userId, DetailsDTO detailsDTO, MultipartFile logo) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Details details = user.getDetails();
        if (details == null) {
            details = new Details();
            details.setUser(user);
        }

        mapDetailsDTOToDetails(detailsDTO, details);

        if (logo != null && !logo.isEmpty()) {
            String logoUrl = logoStorageService.uploadLogo(logo, userId);
            details.setLogoUrl(logoUrl);
        } else if (details.getLogoUrl() == null) {
            details.setLogoUrl(defaultLogoUrl);
        }

        detailsRepository.save(details);
        return mapDetailsToDetailsDTO(details);
    }

    @Override
    public void deleteDetails(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Details details = user.getDetails();
            if (details != null) {
                detailsRepository.delete(details);
                user.setDetails(null);
                userRepository.save(user);
            }
        }
    }

    @Override
    public DetailsDTO getDetailsByUserId(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Details details = user.getDetails();
            if (details != null) {
                return mapDetailsToDetailsDTO(details);
            }
        }
        return null;
    }

    @Override
    public DetailsDTO getDetailsById(Long id) {
        Optional<Details> optionalDetails = detailsRepository.findById(id);
        return optionalDetails.map(this::mapDetailsToDetailsDTO).orElse(null);
    }

    @Override
    public List<DetailsDTO> getAllDetails() {
        List<Details> detailsList = detailsRepository.findAll();
        return detailsList.stream().map(this::mapDetailsToDetailsDTO).collect(Collectors.toList());
    }

    private void mapDetailsDTOToDetails(DetailsDTO detailsDTO, Details details) {
        details.setName(detailsDTO.getName());
        details.setAddressLine1(detailsDTO.getAddressLine1());
        details.setAddressLine2(detailsDTO.getAddressLine2());
        details.setCity(detailsDTO.getCity());
        details.setState(detailsDTO.getState());
        details.setPincode(detailsDTO.getPincode());
        details.setPhoneNumber(detailsDTO.getPhoneNumber());
        details.setDlNo1(detailsDTO.getDlNo1());
        details.setDlNo2(detailsDTO.getDlNo2());
        details.setFssaiReg(detailsDTO.getFssaiReg());
        details.setGstin(detailsDTO.getGstin());
        details.setBankName(detailsDTO.getBankName());
        details.setAccountNumber(detailsDTO.getAccountNumber());
        details.setIfscCode(detailsDTO.getIfscCode());
        details.setLogoUrl(detailsDTO.getLogoUrl());
    }

    private DetailsDTO mapDetailsToDetailsDTO(Details details) {
        DetailsDTO dto = new DetailsDTO();
        dto.setUserId(details.getUser().getId());
        dto.setName(details.getName());
        dto.setAddressLine1(details.getAddressLine1());
        dto.setAddressLine2(details.getAddressLine2());
        dto.setCity(details.getCity());
        dto.setState(details.getState());
        dto.setPincode(details.getPincode());
        dto.setPhoneNumber(details.getPhoneNumber());
        dto.setDlNo1(details.getDlNo1());
        dto.setDlNo2(details.getDlNo2());
        dto.setFssaiReg(details.getFssaiReg());
        dto.setGstin(details.getGstin());
        dto.setBankName(details.getBankName());
        dto.setAccountNumber(details.getAccountNumber());
        dto.setIfscCode(details.getIfscCode());
        dto.setLogoUrl(details.getLogoUrl());
        return dto;
    }
}
			