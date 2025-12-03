package com.gst.billingandstockmanagement.services.postal;

import com.gst.billingandstockmanagement.dto.AddressLookupDTO;

import java.util.List;

public interface PostalService {

    // 1. Pincode Lookup (Autofill)
    AddressLookupDTO lookupPincode(String pincode);

    // 2. Get All States
    List<String> getAllDistinctStates();

    // 3. Get Cities by State
    List<String> getDistinctDistrictsByState(String state);

    // 4. Get State by City/District
    String findStateByDistrict(String district);

    // 5. Get Addresses (Pincodes/City/State) by City and State
    List<AddressLookupDTO> getAddressesByDistrictAndState(String district, String state);
}