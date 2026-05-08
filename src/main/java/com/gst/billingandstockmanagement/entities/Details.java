package com.gst.billingandstockmanagement.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "details")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "user")
public class Details {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "show_qr_on_bill", nullable = false, columnDefinition = "boolean default false")
    private boolean showQrOnBill = false;

    @Column(name = "tax_mode", nullable = false)
    private String taxMode = "CGST_SGST";
}