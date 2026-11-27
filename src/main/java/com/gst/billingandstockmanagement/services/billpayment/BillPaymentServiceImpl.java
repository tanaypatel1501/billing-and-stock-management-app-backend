package com.gst.billingandstockmanagement.services.billpayment;

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

import com.gst.billingandstockmanagement.dto.BillBalanceDTO;
import com.gst.billingandstockmanagement.dto.BillPaymentDTO;
import com.gst.billingandstockmanagement.entities.Bill;
import com.gst.billingandstockmanagement.entities.BillPayment;
import com.gst.billingandstockmanagement.repository.BillPaymentRepository;
import com.gst.billingandstockmanagement.repository.BillRepository;

@Service
public class BillPaymentServiceImpl implements BillPaymentService {

    private static final Set<String> MODES = Set.of("CASH", "UPI", "BANK", "CHEQUE", "OTHER");
    private static final double EPS = 0.005;
    private static final int MAX_BATCH = 200;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BillPaymentRepository billPaymentRepository;

    @Override
    @Transactional(readOnly = true)
    public BillBalanceDTO getBalance(Long userId, Long billId) {
        Bill bill = loadOwned(userId, billId, false);
        return balanceWithPayments(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillBalanceDTO> getBalances(Long userId, List<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return List.of();
        }
        if (billIds.size() > MAX_BATCH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Too many bills requested at once (max " + MAX_BATCH + ")");
        }
        Set<Long> ids = new HashSet<>(billIds);

        Map<Long, Double> paidByBill = new HashMap<>();
        for (Object[] row : billPaymentRepository.sumByBillIds(ids)) {
            paidByBill.put(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
        }

        List<BillBalanceDTO> result = new ArrayList<>();
        for (Bill bill : billRepository.findAllById(ids)) {
            // Bills of other users are skipped silently
            if (bill.getUser() == null || !userId.equals(bill.getUser().getId())) {
                continue;
            }
            result.add(toBalance(bill, paidByBill.getOrDefault(bill.getId(), 0.0), null));
        }
        return result;
    }

    @Override
    @Transactional
    public BillBalanceDTO addPayment(Long userId, Long billId, BillPaymentDTO request) {
        Bill bill = loadOwned(userId, billId, true);
        requireNotReverted(bill);

        if (request == null || request.getAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is required");
        }
        double amount = round2(request.getAmount());
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }

        double total = totalOf(bill);
        double paidSoFar = round2(billPaymentRepository.sumByBillId(billId));
        double outstanding = Math.max(0, round2(total - paidSoFar));
        if (amount > outstanding + EPS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(Locale.ROOT,
                    "Payment of %.2f exceeds the outstanding balance of %.2f", amount, outstanding));
        }

        LocalDate today = LocalDate.now(IST);
        LocalDate date = request.getPaymentDate() != null ? request.getPaymentDate() : today;
        if (date.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment date cannot be in the future");
        }

        String mode = request.getMode() == null || request.getMode().isBlank()
                ? "CASH" : request.getMode().trim().toUpperCase(Locale.ROOT);
        if (!MODES.contains(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment mode: " + mode);
        }

        BillPayment payment = new BillPayment();
        payment.setUser(bill.getUser());
        payment.setBill(bill);
        payment.setAmount(amount);
        payment.setPaymentDate(date);
        payment.setMode(mode);
        payment.setReference(cleanText(request.getReference(), 100, "Reference"));
        payment.setNotes(cleanText(request.getNotes(), 255, "Notes"));
        billPaymentRepository.save(payment);

        syncPaidFlag(bill);
        return balanceWithPayments(bill);
    }

    @Override
    @Transactional
    public BillBalanceDTO deletePayment(Long userId, Long billId, Long paymentId) {
        Bill bill = loadOwned(userId, billId, true);
        requireNotReverted(bill);

        BillPayment payment = billPaymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getBill() == null || !billId.equals(payment.getBill().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        billPaymentRepository.delete(payment);
        billPaymentRepository.flush();

        syncPaidFlag(bill);
        return balanceWithPayments(bill);
    }

    @Override
    @Transactional
    public BillBalanceDTO settleRemaining(Long userId, Long billId) {
        Bill bill = loadOwned(userId, billId, true);
        requireNotReverted(bill);

        double total = totalOf(bill);
        double paidSoFar = round2(billPaymentRepository.sumByBillId(billId));
        double outstanding = Math.max(0, round2(total - paidSoFar));

        if (outstanding > EPS) {
            BillPayment payment = new BillPayment();
            payment.setUser(bill.getUser());
            payment.setBill(bill);
            payment.setAmount(outstanding);
            payment.setPaymentDate(LocalDate.now(IST));
            payment.setMode("OTHER"); // the real mode is unknown here, so it is not guessed
            payment.setNotes("Marked as paid");
            billPaymentRepository.save(payment);
        }

        syncPaidFlag(bill);
        return balanceWithPayments(bill);
    }

    @Override
    @Transactional
    public BillBalanceDTO clearPayments(Long userId, Long billId) {
        Bill bill = loadOwned(userId, billId, true);
        requireNotReverted(bill);

        billPaymentRepository.deleteAll(billPaymentRepository.findByBillIdOrderByPaymentDateAscIdAsc(billId));
        billPaymentRepository.flush();

        syncPaidFlag(bill);
        return balanceWithPayments(bill);
    }

    // ---------------------------------------------------------------- helpers

    private Bill loadOwned(Long userId, Long billId, boolean lock) {
        Bill bill = (lock ? billRepository.findByIdForUpdate(billId) : billRepository.findById(billId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
        if (bill.getUser() == null || !userId.equals(bill.getUser().getId())) {
            throw new AccessDeniedException("You do not have access to this resource.");
        }
        return bill;
    }

    private void requireNotReverted(Bill bill) {
        if (bill.isReverted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This bill has been reverted");
        }
    }

    // Keeps bill.paid equal to "fully covered by payments", so paidOnly filters and the chatbot keep working
    private void syncPaidFlag(Bill bill) {
        double total = totalOf(bill);
        double paid = round2(billPaymentRepository.sumByBillId(bill.getId()));
        boolean fullyPaid = total > EPS && paid >= total - EPS;
        if (bill.isPaid() != fullyPaid) {
            bill.setPaid(fullyPaid);
            billRepository.save(bill);
        }
    }

    private BillBalanceDTO balanceWithPayments(Bill bill) {
        List<BillPayment> rows = billPaymentRepository.findByBillIdOrderByPaymentDateAscIdAsc(bill.getId());
        double paid = 0.0;
        List<BillPaymentDTO> dtos = new ArrayList<>();
        for (BillPayment p : rows) {
            paid += p.getAmount() != null ? p.getAmount() : 0.0;
            dtos.add(toDTO(p));
        }
        return toBalance(bill, paid, dtos);
    }

    private BillBalanceDTO toBalance(Bill bill, double paidRaw, List<BillPaymentDTO> payments) {
        double total = totalOf(bill);
        double paid = round2(paidRaw);
        double outstanding = Math.max(0, round2(total - paid));

        String status;
        if (total > EPS && outstanding <= EPS) {
            status = "PAID";
        } else if (paid > EPS) {
            status = "PARTIAL";
        } else {
            status = "UNPAID";
        }

        BillBalanceDTO dto = new BillBalanceDTO();
        dto.setBillId(bill.getId());
        dto.setTotalAmount(total);
        dto.setPaidAmount(paid);
        dto.setOutstandingAmount(outstanding);
        dto.setStatus(status);
        dto.setPayments(payments);
        return dto;
    }

    private BillPaymentDTO toDTO(BillPayment p) {
        BillPaymentDTO dto = new BillPaymentDTO();
        dto.setId(p.getId());
        dto.setBillId(p.getBill().getId());
        dto.setAmount(p.getAmount());
        dto.setPaymentDate(p.getPaymentDate());
        dto.setMode(p.getMode());
        dto.setReference(p.getReference());
        dto.setNotes(p.getNotes());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }

    private static double totalOf(Bill bill) {
        return round2(bill.getTotalAmount() != null ? bill.getTotalAmount() : 0.0);
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