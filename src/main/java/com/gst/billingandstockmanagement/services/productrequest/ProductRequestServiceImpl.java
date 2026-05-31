package com.gst.billingandstockmanagement.services.productrequest;

import com.gst.billingandstockmanagement.dto.ProductRequestDTO;
import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.entities.ProductRequest;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.enums.ProductRequestStatus;
import com.gst.billingandstockmanagement.repository.ProductRepository;
import com.gst.billingandstockmanagement.repository.ProductRequestRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;
import com.gst.billingandstockmanagement.services.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductRequestServiceImpl implements ProductRequestService {

    @Autowired
    private ProductRequestRepository requestRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.client.url}")
    private String clientUrl;

    // ─────────────────────────────────────────────────────────────
    // USER: submit a new request
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductRequestDTO submitRequest(ProductRequestDTO dto, Long userId) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        ProductRequest request = new ProductRequest();
        request.setName(dto.getName().trim());
        request.setHSN(dto.getHSN());
        request.setMRP(dto.getMRP());
        request.setCGST(dto.getCGST());
        request.setSGST(dto.getSGST());
        request.setPacking(dto.getPacking());
        request.setNotes(dto.getNotes());
        request.setStatus(ProductRequestStatus.PENDING);
        request.setRequestedBy(user);

        ProductRequest saved = requestRepository.save(request);

        // Notify the user that their request was received
        sendRequestReceivedEmail(user, saved);

        return mapToDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // USER: my requests
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<ProductRequestDTO> getRequestsByUser(Long userId) {
        return requestRepository.findByRequestedByIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN: list views
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<ProductRequestDTO> getPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(ProductRequestStatus.PENDING)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductRequestDTO> getAllRequests() {
        return requestRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN: approve
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductRequestDTO approveRequest(Long requestId, String adminNotes) {
        ProductRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != ProductRequestStatus.PENDING) {
            throw new IllegalStateException("Request is already " + request.getStatus());
        }

        // Create the actual product
        Product product = buildProductFromRequest(request);
        productRepository.save(product);

        // Update request status
        request.setStatus(ProductRequestStatus.APPROVED);
        request.setAdminNotes(adminNotes);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);

        // Email the user
        sendApprovalEmail(request.getRequestedBy(), request, product);

        return mapToDTO(request);
    }

    // ─────────────────────────────────────────────────────────────
    // ADMIN: reject
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductRequestDTO rejectRequest(Long requestId, String adminNotes) {
        ProductRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != ProductRequestStatus.PENDING) {
            throw new IllegalStateException("Request is already " + request.getStatus());
        }

        request.setStatus(ProductRequestStatus.REJECTED);
        request.setAdminNotes(adminNotes);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);

        sendRejectionEmail(request.getRequestedBy(), request);

        return mapToDTO(request);
    }

    // ─────────────────────────────────────────────────────────────
    // SCHEDULED AUTO-APPROVAL
    // Runs every 30 minutes. Approves PENDING requests that pass
    // basic validation (name not blank, not an exact duplicate).
    // ─────────────────────────────────────────────────────────────

    @Override
    @Scheduled(fixedDelayString = "${app.product-request.auto-approve-delay-ms:1800000}")
    @Transactional
    public void autoApprovePendingRequests() {
        List<ProductRequest> pending =
                requestRepository.findByStatusOrderByCreatedAtAsc(ProductRequestStatus.PENDING);

        for (ProductRequest request : pending) {
            try {
                autoProcessRequest(request);
            } catch (Exception e) {
                // Log but don't abort the whole batch
                System.err.println("[AutoApprove] Failed to process request id="
                        + request.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Auto-approval rules:
     * APPROVE  — name is present AND no product with the same name+packing already exists
     * REJECT   — exact duplicate product already exists
     */
    private void autoProcessRequest(ProductRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();

        if (name.isEmpty()) {
            // Reject: no product name
            request.setStatus(ProductRequestStatus.REJECTED);
            request.setAdminNotes("Auto-rejected: product name is missing.");
            request.setReviewedAt(LocalDateTime.now());
            requestRepository.save(request);
            sendRejectionEmail(request.getRequestedBy(), request);
            return;
        }

        // Check for an exact name+packing duplicate (case-insensitive)
        String packing = request.getPacking() == null ? "" : request.getPacking().trim();
        boolean duplicateExists = productRepository
                .findAll()
                .stream()
                .anyMatch(p ->
                        p.getName() != null
                                && p.getName().equalsIgnoreCase(name)
                                && (p.getPacking() == null ? "" : p.getPacking().trim()).equalsIgnoreCase(packing)
                );

        if (duplicateExists) {
            request.setStatus(ProductRequestStatus.REJECTED);
            request.setAdminNotes(
                    "Auto-rejected: a product with this name and packing already exists in the catalogue.");
            request.setReviewedAt(LocalDateTime.now());
            requestRepository.save(request);
            sendRejectionEmail(request.getRequestedBy(), request);
            return;
        }

        // All checks passed → approve
        Product product = buildProductFromRequest(request);
        productRepository.save(product);

        request.setStatus(ProductRequestStatus.APPROVED);
        request.setAdminNotes("Auto-approved.");
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);

        sendApprovalEmail(request.getRequestedBy(), request, product);
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private ProductRequest getRequestOrThrow(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product request not found: " + id));
    }

    private Product buildProductFromRequest(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setHSN(request.getHSN());
        product.setMRP(request.getMRP());
        product.setCGST(request.getCGST());
        product.setSGST(request.getSGST());
        product.setPacking(request.getPacking());
        return product;
    }

    // ── Email helpers ──────────────────────────────────────────

    private void sendRequestReceivedEmail(User user, ProductRequest request) {
        StringBuilder content = new StringBuilder();
        content.append("<h2 style='margin:0 0 20px; font-size:24px; font-weight:800; color:#000;'>Product Request Received</h2>");
        content.append("<p style='margin:0 0 16px; font-size:15px; color:#333; line-height:1.6;'>Hello ").append(user.getFirstname()).append(",</p>");
        content.append("<p style='margin:0 0 20px; font-size:15px; color:#333; line-height:1.6;'>Your request for <strong>").append(request.getName()).append("</strong> has been received and is currently under review by our administration team.</p>");

        content.append("<div style='margin:20px 0; padding:15px; background-color:#f9f9f9; border-left:4px solid #48e3cc; border-radius:4px;'>");
        content.append("<p style='margin:0 0 6px; font-size:14px; color:#666;'><strong>Details Submitted:</strong></p>");
        content.append("<p style='margin:0; font-size:14px; color:#333;'>")
                .append("<strong>Product:</strong> ").append(request.getName())
                .append(request.getPacking() != null ? " (" + request.getPacking() + ")" : "")
                .append("</p></div>");

        content.append("<p style='margin:0 0 20px; font-size:15px; color:#333; line-height:1.6;'>We will notify you via email the moment your request has been processed.</p>");

        String finalBody = wrapInTemplate(content.toString());
        emailService.sendEmail(user.getEmail(), "Product Request Received – " + request.getName(), finalBody);
    }

    private void sendApprovalEmail(User user, ProductRequest request, Product product) {
        StringBuilder content = new StringBuilder();
        content.append("<h2 style='margin:0 0 20px; font-size:24px; font-weight:800; color:#000;'>Product Request Approved</h2>");
        content.append("<p style='margin:0 0 16px; font-size:15px; color:#333; line-height:1.6;'>Hello ").append(user.getFirstname()).append(",</p>");
        content.append("<p style='margin:0 0 20px; font-size:15px; color:#333; line-height:1.6;'>Great news! Your product request has been successfully approved and is now active within the master catalogue:</p>");

        /* Catalog Info Table Block */
        content.append("<div style='margin-bottom:30px;'>");
        content.append("<h3 style='color:#36c5b0; font-size:16px; margin-bottom:10px;'>✅ Added to Catalogue</h3>");
        content.append("<table width='100%' cellspacing='0' cellpadding='0' style='border:1px solid #eee; border-radius:8px; overflow:hidden;'>")
                .append("<tr style='background-color:#f9f9f9;'>")
                .append("<th align='left' style='padding:12px; font-size:12px; color:#999; text-transform:uppercase;'>Property</th>")
                .append("<th align='left' style='padding:12px; font-size:12px; color:#999; text-transform:uppercase;'>Details</th>")
                .append("</tr>")
                .append("<tr>")
                .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; font-weight:600;'>Product Name</td>")
                .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; color:#333;'>").append(product.getName()).append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; font-weight:600;'>Packing</td>")
                .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; color:#333;'>").append(product.getPacking() != null ? product.getPacking() : "N/A").append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; font-weight:600;'>HSN Code</td>")
                .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; color:#333;'>").append(product.getHSN() != null ? product.getHSN() : "N/A").append("</td>")
                .append("</tr>");

        if (product.getMRP() != null) {
            tableAppendRow(content, "MRP", "₹" + product.getMRP());
        }
        if (product.getCGST() != null) {
            tableAppendRow(content, "CGST", product.getCGST() + "%");
        }
        if (product.getSGST() != null) {
            tableAppendRow(content, "SGST", product.getSGST() + "%");
        }

        content.append("</table></div>");

        if (request.getAdminNotes() != null && !request.getAdminNotes().isBlank()) {
            content.append("<div style='margin:20px 0; padding:15px; background-color:#f9f9f9; border-left:4px solid #36c5b0; border-radius:4px;'>");
            content.append("<p style='margin:0; font-size:14px; color:#555; line-height:1.5;'><strong>Administrator Remarks:</strong> ").append(request.getAdminNotes()).append("</p>");
            content.append("</div>");
        }

        /* CTA Button */
        content.append("<table role='presentation' width='100%' style='margin:30px 0;'>")
                .append("<tr><td align='center'>")
                .append("<a href='").append(clientUrl).append("/stock' ")
                .append("style='display:inline-block; background:linear-gradient(135deg,#48e3cc 0%,#36c5b0 100%);")
                .append("color:#000; padding:16px 40px; text-decoration:none; border-radius:12px; ")
                .append("font-weight:700; font-size:16px; box-shadow:0 4px 15px rgba(72,227,204,0.3);'>")
                .append("Update Inventory Stock Now</a>")
                .append("</td></tr></table>");

        String finalBody = wrapInTemplate(content.toString());
        emailService.sendEmail(user.getEmail(), "Product Approved & Added – " + product.getName(), finalBody);
    }

    private void sendRejectionEmail(User user, ProductRequest request) {
        StringBuilder content = new StringBuilder();
        content.append("<h2 style='margin:0 0 20px; font-size:24px; font-weight:800; color:#000;'>Product Request Update</h2>");
        content.append("<p style='margin:0 0 16px; font-size:15px; color:#333; line-height:1.6;'>Hello ").append(user.getFirstname()).append(",</p>");
        content.append("<p style='margin:0 0 20px; font-size:15px; color:#333; line-height:1.6;'>We are writing to inform you that your suggestion for adding <strong>").append(request.getName()).append("</strong> has been reviewed and was not approved for inclusion in the master catalogue at this time.</p>");

        if (request.getAdminNotes() != null && !request.getAdminNotes().isBlank()) {
            content.append("<div style='margin:25px 0; padding:20px; background-color:#fff5f5; border-left:4px solid #c92a2a; border-radius:8px;'>");
            content.append("<h3 style='color:#c92a2a; font-size:15px; margin:0 0 8px 0; font-weight:700;'>🚨 Reason for Non-Approval:</h3>");
            content.append("<p style='margin:0; font-size:14px; color:#2b2b2b; line-height:1.5;'>").append(request.getAdminNotes()).append("</p>");
            content.append("</div>");
        }

        content.append("<p style='margin:20px 0 0; font-size:14px; color:#666; line-height:1.6;'>If you believe this decision was made in error or if critical information was left out of the initial application details, please contact your system administrator.</p>");

        String finalBody = wrapInTemplate(content.toString());
        emailService.sendEmail(user.getEmail(), "Product Request Update – " + request.getName(), finalBody);
    }

    // ── Helper for dynamic table row insertions ────────────────
    private void tableAppendRow(StringBuilder sb, String property, String value) {
        sb.append("<tr>")
                .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; font-weight:600;'>").append(property).append("</td>")
                .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; color:#333;'>").append(value).append("</td>")
                .append("</tr>");
    }

    // ── Unified Blueprint Wrapper Template ─────────────────────
    private String wrapInTemplate(String content) {
        String uniqueId = UUID.randomUUID().toString();
        String formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm:ss a"));

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>" +
                "<body style='margin:0; padding:0; background-color:#efefef; font-family:-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Arial, sans-serif;'>" +
                "<div style='display:none; max-height:0; overflow:hidden;'>Catalogue update from GST Medicose. Security Token ID: " + uniqueId + "</div>" +
                "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background-color:#f4f7f6;'>" +
                "<tr><td align='center' style='padding:40px 10px;'>" +
                "<div style='max-width:600px; width:100%; background:#ffffff; border-radius:16px; box-shadow:0 4px 20px rgba(0,0,0,0.05); border:1px solid #e1e8e5; overflow:hidden;'>" +

                /* System Header Block */
                "<table role='presentation' width='100%' cellspacing='0' cellpadding='0'><tr>" +
                "<td bgcolor='#121212' align='center' style='padding:45px 20px;'>" +
                "<h1 style='margin:0; font-size:32px; font-weight:800; color:#ffffff;'>GST <span style='color:#48e3cc;'>Medicose</span></h1>" +
                "<p style='margin:10px 0 0; font-size:12px; color:#cccccc;'>Management System</p></td></tr></table>" +

                /* Injected Dynamic Section Content Area */
                "<div style='padding:40px 30px;'>" + content + "</div>" +

                /* Unified Identity System Footer Block */
                "<div style='background:#fafafa; padding:40px; text-align:center; border-top:1px solid #f0f0f0;'>" +
                "<p style='margin:0 0 10px; font-size:13px; color:#666;'>Need help? Contact us at <a href='mailto:gstmedicose+support@gmail.com' style='color:#48e3cc; text-decoration:none; font-weight:600'>gstmedicose+support@gmail.com</a></p>" +
                "<p style='margin:0; font-size:12px; color:#bbb;'>Report generated on: " + formattedDateTime + " | ID: " + uniqueId.substring(0, 8) + "<br>&copy; 2026 GST Medicose. All Rights Reserved.</p>" +
                "</div></div></td></tr></table></body></html>";
    }

    // ── DTO mapper ─────────────────────────────────────────────

    private ProductRequestDTO mapToDTO(ProductRequest r) {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setHSN(r.getHSN());
        dto.setMRP(r.getMRP());
        dto.setCGST(r.getCGST());
        dto.setSGST(r.getSGST());
        dto.setPacking(r.getPacking());
        dto.setNotes(r.getNotes());
        dto.setStatus(r.getStatus());
        dto.setAdminNotes(r.getAdminNotes());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setReviewedAt(r.getReviewedAt());

        if (r.getRequestedBy() != null) {
            User u = r.getRequestedBy();
            dto.setRequestedById(u.getId());
            dto.setRequestedByName(u.getFirstname() + " " + u.getLastname());
            dto.setRequestedByEmail(u.getEmail());
        }

        return dto;
    }
}