package com.gst.billingandstockmanagement.services.pdf;

public interface PdfService {
    byte[] generateInvoicePdf(Long billId) throws Exception;
}