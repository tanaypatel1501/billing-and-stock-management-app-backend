package com.gst.billingandstockmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressLookupDTO {
    private String pincode;
    private String district;
    private String statename;

    // Constructor to support JPQL constructor expression when p.pincode is an Integer
    public AddressLookupDTO(Integer pincode, String district, String statename) {
        this.pincode = pincode == null ? null : pincode.toString();
        this.district = district;
        this.statename = statename;
    }
}