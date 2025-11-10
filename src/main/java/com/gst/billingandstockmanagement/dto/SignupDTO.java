package com.gst.billingandstockmanagement.dto;

import com.gst.billingandstockmanagement.enums.UserRole;

import lombok.Data;

@Data
public class SignupDTO {
	
	private String firstname;
    private String lastname;
    private String email;
    private String password;
    private UserRole userRole;
}
