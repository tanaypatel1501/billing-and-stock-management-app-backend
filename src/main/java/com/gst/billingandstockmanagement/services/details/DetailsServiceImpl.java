package com.gst.billingandstockmanagement.services.details;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gst.billingandstockmanagement.dto.DetailsDTO;
import com.gst.billingandstockmanagement.entities.Details;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.DetailsRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DetailsServiceImpl implements DetailsService {

    @Autowired
    private DetailsRepository detailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public DetailsDTO createDetails(DetailsDTO detailsDTO) {
        Details details = new Details();
        Optional<User> optionalUser = userRepository.findById(detailsDTO.getUserId());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            details.setUser(user);
            mapDetailsDTOToDetails(detailsDTO, details);
            Details savedDetails = detailsRepository.save(details);
            return mapDetailsToDetailsDTO(savedDetails);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    @Override
    public DetailsDTO updateDetails(Long userId, DetailsDTO detailsDTO) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Details details = user.getDetails();

            if (details == null) {
                details = new Details();
                details.setUser(user);
            }

            mapDetailsDTOToDetails(detailsDTO, details);
            user.setDetails(details);
            userRepository.save(user);
            return mapDetailsToDetailsDTO(details);
        } else {
            throw new RuntimeException("User not found");
        }
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
    }

    private DetailsDTO mapDetailsToDetailsDTO(Details details) {
        DetailsDTO detailsDTO = new DetailsDTO();
        detailsDTO.setUserId(details.getUser().getId());
        mapDetailsToDetailsDTO(details, detailsDTO);
        return detailsDTO;
    }

    private void mapDetailsToDetailsDTO(Details details, DetailsDTO detailsDTO) {
        detailsDTO.setName(details.getName());
        detailsDTO.setAddressLine1(details.getAddressLine1());
        detailsDTO.setAddressLine2(details.getAddressLine2());
        detailsDTO.setCity(details.getCity());
        detailsDTO.setState(details.getState());
        detailsDTO.setPincode(details.getPincode());
        detailsDTO.setPhoneNumber(details.getPhoneNumber());
        detailsDTO.setDlNo1(details.getDlNo1());
        detailsDTO.setDlNo2(details.getDlNo2());
        detailsDTO.setFssaiReg(details.getFssaiReg());
        detailsDTO.setGstin(details.getGstin());
        detailsDTO.setBankName(details.getBankName());
        detailsDTO.setAccountNumber(details.getAccountNumber());
        detailsDTO.setIfscCode(details.getIfscCode());
    }

}
			