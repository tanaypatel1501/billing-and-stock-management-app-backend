package com.gst.billingandstockmanagement.dto.sales;

// One open invoice (outstanding above zero): a bill (grouped by customer) or a purchase (grouped by supplier)
public interface OutstandingRow {
    Number getPartyId();        // customer or supplier id; null when not linked to a saved record
    String getPartyName();
    String getInvoiceDate();    // yyyy-MM-dd
    Number getTotal();
    Number getPaid();
}