package com.gst.billingandstockmanagement.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "india_postal_data")
public class IndiaPostalData {

    @Id
    @Column(name = "pincode")
    @EqualsAndHashCode.Include
    private Integer pincode;

    @Column(name = "district")
    private String district;

    @Column(name = "statename")
    private String statename;
}