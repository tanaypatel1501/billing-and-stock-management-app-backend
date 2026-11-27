package com.gst.billingandstockmanagement.services.purchasepayment;

import java.util.List;

import com.gst.billingandstockmanagement.dto.PurchaseBalanceDTO;
import com.gst.billingandstockmanagement.dto.PurchaseDTO;
import com.gst.billingandstockmanagement.dto.PurchasePaymentDTO;
import com.gst.billingandstockmanagement.entities.Purchase;

public interface PurchasePaymentService {

    PurchaseBalanceDTO getBalance(Long userId, Long purchaseId);

    PurchaseBalanceDTO addPayment(Long userId, Long purchaseId, PurchasePaymentDTO request);

    PurchaseBalanceDTO deletePayment(Long userId, Long purchaseId, Long paymentId);

    // Called by PurchaseService inside the "new purchase" transaction. A null request means "not paid yet".
    void recordInitialPayment(Purchase purchase, PurchasePaymentDTO request);

    // List pages: fills paidAmount, outstandingAmount and paymentStatus with one query for the whole page
    void attachBalances(List<PurchaseDTO> purchases);

    // Detail: the same, plus the payment list
    void attachBalanceWithPayments(PurchaseDTO purchase);
}