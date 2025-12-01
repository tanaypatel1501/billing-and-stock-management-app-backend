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
}