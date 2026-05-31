package com.gst.billingandstockmanagement.services.productrequest;

import com.gst.billingandstockmanagement.dto.ProductRequestDTO;

import java.util.List;

public interface ProductRequestService {

    /** User submits a new product request */
    ProductRequestDTO submitRequest(ProductRequestDTO dto, Long userId);

    /** All requests submitted by a user (for their "My Requests" view) */
    List<ProductRequestDTO> getRequestsByUser(Long userId);

    /** All pending requests — admin view */
    List<ProductRequestDTO> getPendingRequests();

    /** All requests (any status) — admin view */
    List<ProductRequestDTO> getAllRequests();

    /** Admin approves → creates the product, emails the user */
    ProductRequestDTO approveRequest(Long requestId, String adminNotes);

    /** Admin rejects → emails the user with reason */
    ProductRequestDTO rejectRequest(Long requestId, String adminNotes);

    /**
     * Scheduled auto-approval:
     * Runs on a timer, approves PENDING requests that pass basic validation.
     */
    void autoApprovePendingRequests();
}