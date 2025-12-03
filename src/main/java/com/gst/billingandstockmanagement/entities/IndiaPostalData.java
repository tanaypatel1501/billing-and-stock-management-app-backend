package com.gst.billingandstockmanagement.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "india_postal_data")
public class IndiaPostalData {

    @Id
    @Column(name = "pincode")
    private Integer  pincode;

    @Column(name = "district")
    private String district;

    @Column(name = "statename")
    private String statename;
}