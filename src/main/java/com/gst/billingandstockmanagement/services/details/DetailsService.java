package com.gst.billingandstockmanagement.services.details;

import com.gst.billingandstockmanagement.dto.DetailsDTO;
import org.springframework.web.multipart.MultipartFile;

public interface DetailsService {
    DetailsDTO createDetails(DetailsDTO detailsDTO, MultipartFile logo);
    DetailsDTO updateDetails(DetailsDTO detailsDTO, MultipartFile logo);
    void deleteDetails();
	DetailsDTO getDetailsByUserId();
}
