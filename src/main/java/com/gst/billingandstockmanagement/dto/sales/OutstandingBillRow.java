package com.gst.billingandstockmanagement.dto.sales;

// One open bill (outstanding above zero), as returned by BillRepository.findOutstandingBills
public interface OutstandingBillRow {
    Number getBillId();
    Number getPurchaserId();      // null when the bill is not linked to a saved customer
    String getCustomerName();
    String getInvoiceDate();      // yyyy-MM-dd
    Number getTotal();
    Number getPaid();
}