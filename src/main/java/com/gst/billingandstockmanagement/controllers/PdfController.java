package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.dto.BillDTO;
import com.gst.billingandstockmanagement.security.SecurityUtils;
import com.gst.billingandstockmanagement.services.bill.BillService;
import com.gst.billingandstockmanagement.services.pdf.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private BillService billService;

    private final ExecutorService pdfExecutor = Executors.newFixedThreadPool(4);

    @GetMapping("/bill/{billId}")
    public ResponseEntity<byte[]> getBillPdf(@PathVariable Long billId) {
        BillDTO bill = billService.getBillById(billId);
        if (bill == null) {
            return ResponseEntity.notFound().build();
        }
        SecurityUtils.requireOwnership(bill.getUserId());

        try {
            byte[] pdfBytes = pdfService.generateInvoicePdf(billId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", buildInvoiceFilename(bill, new HashSet<>()));
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/bills/zip")
    public ResponseEntity<byte[]> getBillsZip(@RequestBody List<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<BillDTO> bills = new java.util.ArrayList<>();
        for (Long billId : billIds) {
            BillDTO bill = billService.getBillById(billId);
            if (bill == null) continue;
            SecurityUtils.requireOwnership(bill.getUserId());
            bills.add(bill);
        }

        if (bills.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<Future<byte[]>> futures = new java.util.ArrayList<>();
            for (BillDTO bill : bills) {
                futures.add(pdfExecutor.submit(() -> pdfService.generateInvoicePdf(bill.getId())));
            }

            Set<String> usedNames = new HashSet<>();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(baos)) {

                for (int i = 0; i < bills.size(); i++) {
                    byte[] pdfBytes = futures.get(i).get(); // preserves original order
                    String filename = buildInvoiceFilename(bills.get(i), usedNames);

                    zos.putNextEntry(new ZipEntry(filename));
                    zos.write(pdfBytes);
                    zos.closeEntry();
                }
                zos.finish();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", "invoices-" + LocalDate.now() + ".zip");

                return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildInvoiceFilename(BillDTO bill, Set<String> usedNames) {
        String purchaser = bill.getPurchaserName() != null
                ? bill.getPurchaserName().replaceAll("[^a-zA-Z0-9 _-]", "").trim()
                : "Unknown";

        String dateStr = bill.getInvoiceDate() != null
                ? new SimpleDateFormat("dd-MM-yyyy").format(bill.getInvoiceDate())
                : "unknown-date";

        String base = "Invoice-" + purchaser + "-" + dateStr;
        String filename = base + ".pdf";

        int suffix = 2;
        while (!usedNames.add(filename)) {
            filename = base + "-" + suffix + ".pdf";
            suffix++;
        }
        return filename;
    }
}