package com.gst.billingandstockmanagement.services.billpayment;

import java.util.List;

import com.gst.billingandstockmanagement.dto.BillBalanceDTO;
import com.gst.billingandstockmanagement.dto.BillPaymentDTO;

public interface BillPaymentService {

    // One bill, with its payment list
    BillBalanceDTO getBalance(Long userId, Long billId);

    // Many bills (the visible page of the Bills list), without payment lists
    List<BillBalanceDTO> getBalances(Long userId, List<Long> billIds);

    BillBalanceDTO addPayment(Long userId, Long billId, BillPaymentDTO request);

    BillBalanceDTO deletePayment(Long userId, Long billId, Long paymentId);

    // Legacy "Mark as Paid": records one payment for whatever is still outstanding
    BillBalanceDTO settleRemaining(Long userId, Long billId);

    // Legacy "unmark paid": removes every payment of the bill
    BillBalanceDTO clearPayments(Long userId, Long billId);
}