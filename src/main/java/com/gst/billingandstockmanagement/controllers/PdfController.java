package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.services.pdf.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @GetMapping("/bill/{billId}")
    public ResponseEntity<byte[]> getBillPdf(@PathVariable Long billId) {
        try {
            byte[] pdfBytes = pdfService.generateInvoicePdf(billId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);

            // "inline" allows the browser to display it in a preview/modal
            // instead of forcing an immediate download
            headers.setContentDispositionFormData("inline", "invoice-" + billId + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}