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
        try {
            Integer pin = Integer.valueOf(pincode.trim());
            IndiaPostalData entity = postalRepository.findByPincode(pin);
            System.out.println("entity:"+entity);
            if (entity == null) return null;
            return new AddressLookupDTO(entity.getPincode().toString(),
                    entity.getDistrict(),
                    entity.getStatename());
        } catch (NumberFormatException e) {
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