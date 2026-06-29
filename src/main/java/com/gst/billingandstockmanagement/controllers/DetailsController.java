package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.DetailsDTO;
import com.gst.billingandstockmanagement.services.details.DetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/details")
public class DetailsController {

    @Autowired
    private DetailsService detailsService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DetailsDTO createDetails(
            @RequestPart("details") DetailsDTO detailsDTO,
            @RequestPart(value = "logo", required = false) MultipartFile logo
    ) {
        return detailsService.createDetails(detailsDTO, logo);
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DetailsDTO updateDetails(
            @RequestPart("details") DetailsDTO detailsDTO,
            @RequestPart(value = "logo", required = false) MultipartFile logo
    ) {
        return detailsService.updateDetails(detailsDTO, logo);
    }

    @DeleteMapping("/delete")
    public void deleteDetails() {
        detailsService.deleteDetails();
    }

    @GetMapping
    public DetailsDTO getDetailsByUserId() {
        return detailsService.getDetailsByUserId();
    }
}