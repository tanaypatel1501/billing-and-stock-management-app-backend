package com.gst.billingandstockmanagement.dto;

import java.util.Map;
import lombok.Data;

@Data
public class BulkRowResult {
    private int rowNumber;
    private Map<String, String> row;
    private String errorMessage;
    private String code;
}

