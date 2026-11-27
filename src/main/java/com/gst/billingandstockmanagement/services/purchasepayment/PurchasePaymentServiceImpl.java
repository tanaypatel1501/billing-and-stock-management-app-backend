package com.gst.billingandstockmanagement.services.purchasepayment;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.gst.billingandstockmanagement.dto.PurchaseBalanceDTO;
import com.gst.billingandstockmanagement.dto.PurchaseDTO;
import com.gst.billingandstockmanagement.dto.PurchasePaymentDTO;
import com.gst.billingandstockmanagement.entities.Purchase;
import com.gst.billingandstockmanagement.entities.PurchasePayment;
import com.gst.billingandstockmanagement.repository.PurchasePaymentRepository;
import com.gst.billingandstockmanagement.repository.PurchaseRepository;

@Service
public class PurchasePaymentServiceImpl implements PurchasePaymentService {

    private static final Set<String> MODES = Set.of("CASH", "UPI", "BANK", "CHEQUE", "OTHER");
    private static final double EPS = 0.005;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PurchasePaymentRepository purchasePaymentRepository;

    @Override
    @Transactional(readOnly = true)
    public PurchaseBalanceDTO getBalance(Long userId, Long purchaseId) {
        Purchase purchase = loadOwned(userId, purchaseId, false);
        return balanceWithPayments(purchase);
    }

    @Override
    @Transactional
    public PurchaseBalanceDTO addPayment(Long userId, Long purchaseId, PurchasePaymentDTO request) {
        Purchase purchase = loadOwned(userId, purchaseId, true);
        if (purchase.isReverted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This purchase has been reverted");
        }
        if (request == null || request.getAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is required");
        }
        double amount = round2(request.getAmount());
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }

        double total = totalOf(purchase);
        double paidSoFar = round2(purchasePaymentRepository.sumByPurchaseId(purchaseId));
        double outstanding = Math.max(0, round2(total - paidSoFar));
        if (amount > outstanding + EPS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(Locale.ROOT,
                    "Payment of %.2f exceeds the outstanding balance of %.2f", amount, outstanding));
        }

        newPayment(purchase, amount, resolveDate(request.getPaymentDate(), LocalDate.now(IST)), request);
        return balanceWithPayments(purchase);
    }

    @Override
    @Transactional
    public PurchaseBalanceDTO deletePayment(Long userId, Long purchaseId, Long paymentId) {
        Purchase purchase = loadOwned(userId, purchaseId, true);
        if (purchase.isReverted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This purchase has been reverted");
        }
        PurchasePayment payment = purchasePaymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getPurchase() == null || !purchaseId.equals(payment.getPurchase().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        purchasePaymentRepository.delete(payment);
        purchasePaymentRepository.flush();
        return balanceWithPayments(purchase);
    }

    @Override
    @Transactional
    public void recordInitialPayment(Purchase purchase, PurchasePaymentDTO request) {
        if (request == null) {
            return; // not paid yet
        }
        double total = totalOf(purchase);
        if (total <= EPS) {
            return; // nothing to pay
        }
        double amount = request.getAmount() == null ? total : round2(request.getAmount());
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than 0");
        }
        if (amount > total + EPS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Locale.ROOT,
                    "Payment of %.2f exceeds the purchase total of %.2f", amount, total));
        }
        // Prepaid purchases are normal, so the payment date defaults to the invoice date
        newPayment(purchase, amount, resolveDate(request.getPaymentDate(), purchase.getInvoiceDate()), request);
    }

    @Override
    @Transactional(readOnly = true)
    public void attachBalances(List<PurchaseDTO> purchases) {
        if (purchases == null || purchases.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (PurchaseDTO p : purchases) {
            ids.add(p.getId());
        }
        Map<Long, Double> paidByPurchase = new HashMap<>();
        for (Object[] row : purchasePaymentRepository.sumByPurchaseIds(ids)) {
            paidByPurchase.put(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
        }
        for (PurchaseDTO p : purchases) {
            applyBalance(p, paidByPurchase.getOrDefault(p.getId(), 0.0));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void attachBalanceWithPayments(PurchaseDTO purchase) {
        List<PurchasePayment> rows = purchasePaymentRepository
                .findByPurchaseIdOrderByPaymentDateAscIdAsc(purchase.getId());
        double paid = 0.0;
        List<PurchasePaymentDTO> dtos = new ArrayList<>();
        for (PurchasePayment p : rows) {
            paid += p.getAmount() != null ? p.getAmount() : 0.0;
            dtos.add(toDTO(p));
        }
        applyBalance(purchase, paid);
        purchase.setPayments(dtos);
    }

    // ---------------------------------------------------------------- helpers

    private Purchase loadOwned(Long userId, Long purchaseId, boolean lock) {
        Purchase purchase = (lock ? purchaseRepository.findByIdForUpdate(purchaseId) : purchaseRepository.findById(purchaseId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found"));
        if (purchase.getUser() == null || !userId.equals(purchase.getUser().getId())) {
            throw new AccessDeniedException("You do not have access to this resource.");
        }
        return purchase;
    }

    private PurchasePayment newPayment(Purchase purchase, double amount, LocalDate date, PurchasePaymentDTO request) {
        String mode = request.getMode() == null || request.getMode().isBlank()
                ? "CASH" : request.getMode().trim().toUpperCase(Locale.ROOT);
        if (!MODES.contains(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment mode: " + mode);
        }
        PurchasePayment payment = new PurchasePayment();
        payment.setUser(purchase.getUser());
        payment.setPurchase(purchase);
        payment.setAmount(amount);
        payment.setPaymentDate(date);
        payment.setMode(mode);
        payment.setReference(cleanText(request.getReference(), 100, "Reference"));
        payment.setNotes(cleanText(request.getNotes(), 255, "Notes"));
        return purchasePaymentRepository.save(payment);
    }

    // Payments may be dated before the invoice (prepaid) but never in the future
    private LocalDate resolveDate(LocalDate requested, LocalDate fallback) {
        LocalDate date = requested != null ? requested : fallback;
        if (date.isAfter(LocalDate.now(IST))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment date cannot be in the future");
        }
        return date;
    }

    private PurchaseBalanceDTO balanceWithPayments(Purchase purchase) {
        List<PurchasePayment> rows = purchasePaymentRepository
                .findByPurchaseIdOrderByPaymentDateAscIdAsc(purchase.getId());
        double paid = 0.0;
        List<PurchasePaymentDTO> dtos = new ArrayList<>();
        for (PurchasePayment p : rows) {
            paid += p.getAmount() != null ? p.getAmount() : 0.0;
            dtos.add(toDTO(p));
        }
        double total = totalOf(purchase);
        double paidR = round2(paid);

        PurchaseBalanceDTO dto = new PurchaseBalanceDTO();
        dto.setPurchaseId(purchase.getId());
        dto.setTotalAmount(total);
        dto.setPaidAmount(paidR);
        dto.setOutstandingAmount(Math.max(0, round2(total - paidR)));
        dto.setStatus(statusOf(total, paidR));
        dto.setPayments(dtos);
        return dto;
    }

    private void applyBalance(PurchaseDTO dto, double paidRaw) {
        double total = round2(dto.getTotalAmount() != null ? dto.getTotalAmount() : 0.0);
        double paid = round2(paidRaw);
        dto.setPaidAmount(paid);
        dto.setOutstandingAmount(Math.max(0, round2(total - paid)));
        dto.setPaymentStatus(statusOf(total, paid));
    }

    private static String statusOf(double total, double paid) {
        double outstanding = Math.max(0, round2(total - paid));
        if (total > EPS && outstanding <= EPS) {
            return "PAID";
        }
        return paid > EPS ? "PARTIAL" : "UNPAID";
    }

    private PurchasePaymentDTO toDTO(PurchasePayment p) {
        PurchasePaymentDTO dto = new PurchasePaymentDTO();
        dto.setId(p.getId());
        dto.setPurchaseId(p.getPurchase().getId());
        dto.setAmount(p.getAmount());
        dto.setPaymentDate(p.getPaymentDate());
        dto.setMode(p.getMode());
        dto.setReference(p.getReference());
        dto.setNotes(p.getNotes());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }

    private static double totalOf(Purchase purchase) {
        return round2(purchase.getTotalAmount() != null ? purchase.getTotalAmount() : 0.0);
    }

    private static String cleanText(String value, int maxLen, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > maxLen) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " must be at most " + maxLen + " characters");
        }
        return v;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}