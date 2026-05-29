package com.gst.billingandstockmanagement.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class StockLogDTO {
    private Long id;
    private Long stockId;
    private String action;
    private String notes;
    private LocalDateTime timestamp;
}