package com.gst.billingandstockmanagement.services.details;

import com.gst.billingandstockmanagement.dto.DetailsDTO;
import com.gst.billingandstockmanagement.entities.Details;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DetailsService {
    DetailsDTO createDetails(Long userId, DetailsDTO detailsDTO, MultipartFile logo);
    DetailsDTO updateDetails(Long userId, DetailsDTO detailsDTO, MultipartFile logo);
    void deleteDetails(Long id);
    DetailsDTO getDetailsById(Long id);
    List<DetailsDTO> getAllDetails();
	DetailsDTO getDetailsByUserId(Long userId);
}
