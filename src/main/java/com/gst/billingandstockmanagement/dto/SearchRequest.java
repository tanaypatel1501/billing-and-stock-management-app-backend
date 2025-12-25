package com.gst.billingandstockmanagement.dto;

import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SearchRequest {
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String direction = "asc";
    private String searchText;
    private Map<String, String> filters;
}

