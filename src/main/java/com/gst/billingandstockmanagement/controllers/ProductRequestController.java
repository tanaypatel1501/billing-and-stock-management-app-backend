package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.ProductRequestDTO;
import com.gst.billingandstockmanagement.services.productrequest.ProductRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-requests")
public class ProductRequestController {

    @Autowired
    private ProductRequestService productRequestService;

    // ── USER ENDPOINTS ─────────────────────────────────────────

    /**
     * POST /api/product-requests/submit?userId={userId}
     * Any authenticated user submits a new product request.
     */
    @PostMapping("/submit")
    public ResponseEntity<ProductRequestDTO> submit(
            @RequestBody ProductRequestDTO dto,
            @RequestParam Long userId) {
        return ResponseEntity.ok(productRequestService.submitRequest(dto, userId));
    }

    /**
     * GET /api/product-requests/my?userId={userId}
     * Returns all requests submitted by the given user.
     */
    @GetMapping("/my")
    public ResponseEntity<List<ProductRequestDTO>> myRequests(@RequestParam Long userId) {
        return ResponseEntity.ok(productRequestService.getRequestsByUser(userId));
    }

    // ── ADMIN ENDPOINTS ────────────────────────────────────────

    /**
     * GET /api/product-requests/pending
     * Admin: list only PENDING requests.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductRequestDTO>> pending() {
        return ResponseEntity.ok(productRequestService.getPendingRequests());
    }

    /**
     * GET /api/product-requests/all
     * Admin: list all requests (any status).
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductRequestDTO>> all() {
        return ResponseEntity.ok(productRequestService.getAllRequests());
    }

    /**
     * POST /api/product-requests/{id}/approve
     * Admin approves a request.
     * Body (optional): { "adminNotes": "Looks good!" }
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRequestDTO> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.getOrDefault("adminNotes", "") : "";
        return ResponseEntity.ok(productRequestService.approveRequest(id, notes));
    }

    /**
     * POST /api/product-requests/{id}/reject
     * Admin rejects a request.
     * Body (optional): { "adminNotes": "Already exists under different name." }
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRequestDTO> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.getOrDefault("adminNotes", "") : "";
        return ResponseEntity.ok(productRequestService.rejectRequest(id, notes));
    }
}