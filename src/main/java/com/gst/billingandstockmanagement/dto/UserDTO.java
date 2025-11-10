package com.gst.billingandstockmanagement.dto;

import com.gst.billingandstockmanagement.enums.UserRole;

import lombok.Data;

@Data
public class UserDTO {
	private Long id;

    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private UserRole userRole;
   
}
