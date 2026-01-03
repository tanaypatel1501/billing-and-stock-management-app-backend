package com.gst.billingandstockmanagement.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.gst.billingandstockmanagement.dto.DetailsDTO;
import com.gst.billingandstockmanagement.services.details.DetailsService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/details")
public class DetailsController {

    @Autowired
    private DetailsService detailsService;

    @PostMapping(
            value = "/create/{userId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public DetailsDTO createDetails(
            @PathVariable Long userId,
            @RequestPart("details") DetailsDTO detailsDTO,
            @RequestPart(value = "logo", required = false) MultipartFile logo
    ) {
        return detailsService.createDetails(userId, detailsDTO, logo);
    }

    @PutMapping(
            value = "/update/{userId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public DetailsDTO updateDetails(
            @PathVariable Long userId,
            @RequestPart("details") DetailsDTO detailsDTO,
            @RequestPart(value = "logo", required = false) MultipartFile logo
    ) {
        return detailsService.updateDetails(userId, detailsDTO, logo);
    }

    @DeleteMapping("/delete/{userId}")
    public void deleteDetails(@PathVariable Long userId) {
        detailsService.deleteDetails(userId);
    }

    @GetMapping("/{userId}")
    public DetailsDTO getDetailsByUserId(@PathVariable Long userId) {
        return detailsService.getDetailsByUserId(userId);
    }

    @GetMapping("/all")
    public List<DetailsDTO> getAllDetails() {
        return detailsService.getAllDetails();
    }
}
