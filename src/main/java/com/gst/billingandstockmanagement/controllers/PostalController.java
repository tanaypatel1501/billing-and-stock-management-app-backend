package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.AddressLookupDTO;
import com.gst.billingandstockmanagement.services.postal.PostalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/postal")
public class PostalController {

    private final PostalService postalService; // Inject the Service interface

    // 1. Pincode Lookup Endpoint
    @GetMapping("/{pincode}")
    public ResponseEntity<?> lookup(@PathVariable String pincode) {

            AddressLookupDTO response = postalService.lookupPincode(pincode);
        System.out.println("response:"+response);
        if (response == null) {
            return ResponseEntity.status(404).body("Pincode not found or invalid");
        }

        return ResponseEntity.ok(response);
    }


    // 2. State List Endpoint
    @GetMapping("/states")
    public ResponseEntity<List<String>> getAllStates() {
        return ResponseEntity.ok(postalService.getAllDistinctStates()); // Delegate to Service
    }

    // 3. City List Endpoint
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCitiesByState(@RequestParam String state) {
        if (state == null || state.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(postalService.getDistinctDistrictsByState(state)); // Delegate to Service
    }

    // 4. Get State by City/District Endpoint
    @GetMapping("/lookup-state")
    public ResponseEntity<?> lookupStateByDistrict(@RequestParam String district) {
        if (district == null || district.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("District parameter is required.");
        }

        String state = postalService.findStateByDistrict(district); // Delegate to Service

        if (state == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("District not found.");
        }

        return ResponseEntity.ok(state);
    }

    // 5. Get Addresses by City/State Endpoint
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressLookupDTO>> getAddressesByCityAndState(
            @RequestParam String district,
            @RequestParam String state) {

        if (district == null || state == null || district.trim().isEmpty() || state.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        List<AddressLookupDTO> addresses = postalService.getAddressesByDistrictAndState(district, state); // Delegate to Service
        return ResponseEntity.ok(addresses);
    }
}