package com.gst.billingandstockmanagement.dto;

import java.util.List;
import lombok.Data;

@Data
public class BulkProductResponse {
    private int processed;
    private int created;
    private int updated;
    private int failed;
    private List<BulkRowResult> errors;
}

