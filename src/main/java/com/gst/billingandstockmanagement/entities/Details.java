package com.gst.billingandstockmanagement.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="detials")
@Data
public class Details {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@OneToOne
	@JoinColumn(name = "user_id")
	private User user;
	
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
