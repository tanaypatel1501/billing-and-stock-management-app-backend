package com.gst.billingandstockmanagement.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.gst.billingandstockmanagement.dto.DetailsDTO;
import com.gst.billingandstockmanagement.services.details.DetailsService;

import java.util.List;

@RestController
@RequestMapping("/api/details")
public class DetailsController {

    @Autowired
    private DetailsService detailsService;

    @PostMapping("/create/{userId}")
    public DetailsDTO createDetails(@RequestBody DetailsDTO detailsDTO) {
        return detailsService.createDetails(detailsDTO);
    }

    @PutMapping("/update/{userId}")
    public DetailsDTO updateDetails(@PathVariable Long userId, @RequestBody DetailsDTO detailsDTO) {
        return detailsService.updateDetails(userId, detailsDTO);
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
