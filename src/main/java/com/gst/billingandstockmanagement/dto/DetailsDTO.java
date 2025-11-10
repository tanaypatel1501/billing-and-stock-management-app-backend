package com.gst.billingandstockmanagement.dto;

import com.gst.billingandstockmanagement.entities.User;

import lombok.Data;

@Data
public class DetailsDTO {
	   	private Long userId; 
	    private String name;
	    private String addressLine1;
	    private String addressLine2;
	    private String city;
	    private String state;
	    private String pincode;
	    private String phoneNumber;
	    private String dlNo1;
	    private String dlNo2;
	    private String fssaiReg;
	    private String gstin;
	    private String bankName;
	    private String accountNumber;
	    private String ifscCode;
}
