package com.gst.billingandstockmanagement.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

import com.gst.billingandstockmanagement.dto.BillDTO;
import com.gst.billingandstockmanagement.dto.BillBalanceDTO;
import com.gst.billingandstockmanagement.dto.BillPaymentDTO;
import com.gst.billingandstockmanagement.dto.SearchRequest;
import com.gst.billingandstockmanagement.security.SecurityUtils;
import com.gst.billingandstockmanagement.services.bill.BillService;
import com.gst.billingandstockmanagement.services.billpayment.BillPaymentService;
import com.gst.billingandstockmanagement.entities.Bill;


@RestController
@RequestMapping("/api/bill")
public class BillController {

    @Autowired
    private BillService billService;

    @Autowired
    private BillPaymentService billPaymentService;

    @PostMapping("/add")
    public ResponseEntity<BillDTO> addBill(@RequestBody BillDTO billDTO) {
        billDTO.setUserId(SecurityUtils.getCurrentUserId());
        BillDTO createdBill = billService.submitBillWithItems(billDTO);
        return new ResponseEntity<>(createdBill, HttpStatus.CREATED);
    }

    @GetMapping("/{billId}")
    public ResponseEntity<BillDTO> getBillById(@PathVariable Long billId) {
        BillDTO dto = billService.getBillById(billId);
        if (dto == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        SecurityUtils.requireOwnership(dto.getUserId());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/user")
    public ResponseEntity<List<BillDTO>> getAllBillsByUser() {
        List<BillDTO> userBills = billService.getAllBillsByUser(SecurityUtils.getCurrentUserId());
        return new ResponseEntity<>(userBills, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{billId}")
    public void deleteBill(@PathVariable Long billId) {
        BillDTO dto = billService.getBillById(billId);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found");
        }
        SecurityUtils.requireOwnership(dto.getUserId());
        billService.deleteBill(billId);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<Bill>> searchBills(@RequestBody SearchRequest request) {
        if (request.getFilters() == null) request.setFilters(new HashMap<>());
        request.getFilters().put("user.id", String.valueOf(SecurityUtils.getCurrentUserId()));
        Page<Bill> p = billService.searchWithPagination(request);
        return ResponseEntity.ok(p);
    }

    @PatchMapping("/{billId}/paid")
    public ResponseEntity<BillBalanceDTO> updatePaidStatus(
            @PathVariable Long billId,
            @RequestParam boolean paid) {
        Long userId = SecurityUtils.getCurrentUserId();
        BillBalanceDTO balance = paid
                ? billPaymentService.settleRemaining(userId, billId)
                : billPaymentService.clearPayments(userId, billId);
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/{billId}/revert")
    public ResponseEntity<Void> revertBill(@PathVariable Long billId) {
        BillDTO dto = billService.getBillById(billId);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found");
        }
        SecurityUtils.requireOwnership(dto.getUserId());
        billService.revertBill(billId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{billId}/balance")
    public ResponseEntity<BillBalanceDTO> getBalance(@PathVariable Long billId) {
        return ResponseEntity.ok(billPaymentService.getBalance(SecurityUtils.getCurrentUserId(), billId));
    }

    // Body: {"billIds":[1,2,3]}. Used by the Bills list for the visible page.
    @PostMapping("/balances")
    public ResponseEntity<List<BillBalanceDTO>> getBalances(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body == null ? null : body.get("billIds");
        return ResponseEntity.ok(billPaymentService.getBalances(SecurityUtils.getCurrentUserId(), ids));
    }

    @PostMapping("/{billId}/payments")
    public ResponseEntity<BillBalanceDTO> addPayment(
            @PathVariable Long billId,
            @RequestBody BillPaymentDTO request) {
        BillBalanceDTO balance = billPaymentService.addPayment(SecurityUtils.getCurrentUserId(), billId, request);
        return new ResponseEntity<>(balance, HttpStatus.CREATED);
    }

    @DeleteMapping("/{billId}/payments/{paymentId}")
    public ResponseEntity<BillBalanceDTO> deletePayment(
            @PathVariable Long billId,
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(
                billPaymentService.deletePayment(SecurityUtils.getCurrentUserId(), billId, paymentId));
    }
}