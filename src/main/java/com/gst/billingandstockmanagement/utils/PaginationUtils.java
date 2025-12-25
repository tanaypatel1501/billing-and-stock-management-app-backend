package com.gst.billingandstockmanagement.utils;

import com.gst.billingandstockmanagement.dto.SearchRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

public class PaginationUtils {

    public static Pageable getPageable(SearchRequest req) {
        // Use null-safe checks with defaults
        int page = (req.getPage() == null) ? 0 : req.getPage();
        int size = (req.getSize() == null) ? 10 : req.getSize();
        // Default sort by 'id' if not provided
        String sortBy = (req.getSortBy() == null || req.getSortBy().isEmpty()) ? "id" : req.getSortBy();
        // Default direction 'asc' if not provided
        String dir = (req.getDirection() == null || req.getDirection().isEmpty()) ? "asc" : req.getDirection();

        // Determine sort direction
        Sort sort = "desc".equalsIgnoreCase(dir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        // Create and return the Pageable object
        return PageRequest.of(page, size, sort);
    }
}