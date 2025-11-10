package com.gst.billingandstockmanagement.services.details;

import com.gst.billingandstockmanagement.dto.DetailsDTO;
import com.gst.billingandstockmanagement.entities.Details;

import java.util.List;

public interface DetailsService {
    DetailsDTO createDetails(DetailsDTO detailsDTO);
    DetailsDTO updateDetails(Long id, DetailsDTO detailsDTO);
    void deleteDetails(Long id);
    DetailsDTO getDetailsById(Long id);
    List<DetailsDTO> getAllDetails();
	DetailsDTO getDetailsByUserId(Long userId);
}
