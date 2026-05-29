package com.gst.billingandstockmanagement.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "stock_log")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StockLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    @JsonBackReference
    private Stock stock;

    private String action;
    private String notes;
    private LocalDateTime timestamp;
}