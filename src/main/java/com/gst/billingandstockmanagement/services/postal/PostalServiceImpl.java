package com.gst.billingandstockmanagement.services.postal;

import com.gst.billingandstockmanagement.dto.AddressLookupDTO;
import com.gst.billingandstockmanagement.entities.IndiaPostalData;
import com.gst.billingandstockmanagement.repository.PostalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostalServiceImpl implements PostalService {

    private final PostalRepository postalRepository;

    @Override
    public AddressLookupDTO lookupPincode(String pincode) {
        if (pincode == null) {
            System.out.println("lookupPincode: received null pincode");
            return null;
        }

        String pinTrim = pincode.trim();
        System.out.println("lookupPincode: raw='" + pincode + "' trimmed='" + pinTrim + "'");

        if (pinTrim.isEmpty()) {
            System.out.println("lookupPincode: empty pincode after trim");
            return null;
        }

        // Basic validation: Indian pincodes are typically 6 digits; allow 1-6 digits to be permissive
        if (!pinTrim.matches("\\d{1,6}")) {
            System.out.println("lookupPincode: invalid pincode format: '" + pinTrim + "'");
            return null;
        }

        try {
            Integer pin = Integer.valueOf(pinTrim);
            IndiaPostalData entity = postalRepository.findByPincode(pin);
            System.out.println("lookupPincode: repository.findByPincode(" + pin + ") returned: " + entity);

            if (entity == null) {
                // Fallback: try string-based lookup (handles leading zeros or DB stored as CHAR)
                try {
                    IndiaPostalData entityStr = postalRepository.findByPincodeString(pinTrim);
                    System.out.println("lookupPincode: fallback findByPincodeString('" + pinTrim + "') returned: " + entityStr);
                    if (entityStr == null) return null;
                    return new AddressLookupDTO(entityStr.getPincode().toString(), entityStr.getDistrict(), entityStr.getStatename());
                } catch (Exception ex) {
                    System.out.println("lookupPincode: exception during findByPincodeString: " + ex.getMessage());
                    return null;
                }
            }

            return new AddressLookupDTO(entity.getPincode().toString(),
                    entity.getDistrict(),
                    entity.getStatename());
        } catch (NumberFormatException e) {
            System.out.println("lookupPincode: NumberFormatException for '" + pinTrim + "' -> " + e.getMessage());
            return null;
        }
    }


    @Override
    public List<String> getAllDistinctStates() {
        return postalRepository.findAllDistinctStates();
    }

    @Override
    public List<String> getDistinctDistrictsByState(String state) {
        return postalRepository.findDistinctDistrictsByState(state);
    }

    @Override
    public String findStateByDistrict(String district) {
        return postalRepository.findStateByDistrict(district);
    }

    @Override
    public List<AddressLookupDTO> getAddressesByDistrictAndState(String district, String state) {
        return postalRepository.findAddressesByDistrictAndState(district, state);
    }
}