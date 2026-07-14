package com.gst.billingandstockmanagement.services.pdf;

import com.gst.billingandstockmanagement.entities.Bill;
import com.gst.billingandstockmanagement.entities.BillItems;
import com.gst.billingandstockmanagement.entities.Details;
import com.gst.billingandstockmanagement.repository.BillRepository;
import com.gst.billingandstockmanagement.repository.DetailsRepository;
import com.gst.billingandstockmanagement.utils.NumberToWordsConverter;
import com.gst.billingandstockmanagement.utils.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.lowagie.text.pdf.BaseFont;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public class PdfServiceImpl implements PdfService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private DetailsRepository detailsRepository;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private QrCodeGenerator qrCodeGenerator;

    @Autowired
    private NumberToWordsConverter numberToWordsConverter;

    @Autowired
    private S3Client s3Client;

    @Value("${tebi.bucket}")
    private String bucketName;

    @Value("${branding.default-logo-url}")
    private String defaultLogoUrl;

    private static final double T1_TOTAL_CONTENT_MM = 197.0;
    private static final double T1_TITLE_MM = 12.0;
    private static final double T1_TOP_SECTION_MM = 58.0;
    private static final double T1_TABLE_HEADER_MM = 8.0;
    private static final double T1_ROW_MM = 8.0;
    private static final double T1_FOOTER_MM = 32.0;

    private static final double T2_TOTAL_CONTENT_MM = 273.0;
    private static final double T2_HEADER_STACK_MM = 84.0;
    private static final double T2_TABLE_HEADER_MM = 8.0;
    private static final double T2_ROW_MM = 7.0;
    private static final double T2_FOOTER_MM = 45.0;

    // ── In-memory logo cache: avoids hitting S3 on every single PDF render.
    private static final Duration LOGO_CACHE_TTL = Duration.ofMinutes(30);
    private final Map<String, CachedLogo> logoCache = new ConcurrentHashMap<>();

    private record CachedLogo(String base64, Instant fetchedAt) {
        boolean isExpired() {
            return Duration.between(fetchedAt, Instant.now()).compareTo(LOGO_CACHE_TTL) > 0;
        }
    }

    private record InvoiceTotals(double subtotal, double discountTotal, double cgstTotal, double sgstTotal) {}

    private InvoiceTotals computeTotals(Bill bill) {
        double subtotal = 0, discount = 0, cgst = 0, sgst = 0;
        List<BillItems> items = bill.getBillItems();
        if (items != null) {
            for (BillItems item : items) {
                double lineBase = item.getRate() * item.getQuantity();
                subtotal += lineBase;
                discount += item.getFree() * item.getRate();
                cgst += lineBase * item.getSnapshotCgst() / 100.0;
                sgst += lineBase * item.getSnapshotSgst() / 100.0;
            }
        }
        return new InvoiceTotals(subtotal, discount, cgst, sgst);
    }

    private volatile String cachedFontPath;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long billId) throws Exception {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        Details details = detailsRepository.findByUserId(bill.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Business details not found"));

        Context context = new Context();
        context.setVariable("bill", bill);
        String purchaserGstin = bill.getGstin();
        String idLabel = getIdentificationLabel(purchaserGstin);
        String idValue = "Aadhaar".equals(idLabel)
                ? formatAadhaar(purchaserGstin)
                : purchaserGstin;

        context.setVariable("purchaserIdLabel", idLabel);
        context.setVariable("purchaserIdValue", idValue);
        context.setVariable("details", details);
        context.setVariable("totalAmountInWords", numberToWordsConverter.convert(bill.getTotalAmount()));

        InvoiceTotals totals = computeTotals(bill);
        context.setVariable("subtotal", totals.subtotal());
        context.setVariable("discountTotal", totals.discountTotal());
        context.setVariable("cgstTotal", totals.cgstTotal());
        context.setVariable("sgstTotal", totals.sgstTotal());

        String templateName = "template1".equals(details.getPreferredTemplate())
                ? "invoice-template"
                : "invoice-template-2";

        int itemCount = bill.getBillItems() != null ? bill.getBillItems().size() : 0;
        int fillerRowCount = "invoice-template".equals(templateName)
                ? computeFillerRows(itemCount, T1_TOTAL_CONTENT_MM, T1_TITLE_MM + T1_TOP_SECTION_MM, T1_TABLE_HEADER_MM, T1_ROW_MM, T1_FOOTER_MM)
                : computeFillerRows(itemCount, T2_TOTAL_CONTENT_MM, T2_HEADER_STACK_MM, T2_TABLE_HEADER_MM, T2_ROW_MM, T2_FOOTER_MM);

        double rowMm = "invoice-template".equals(templateName) ? T1_ROW_MM : T2_ROW_MM;
        context.setVariable("fillerRows", fillerRowCount > 0
                ? IntStream.range(0, fillerRowCount).boxed().toList()
                : Collections.emptyList());

        try {
            String urlToFetch = (details.getLogoUrl() != null && !details.getLogoUrl().isEmpty())
                    ? details.getLogoUrl()
                    : defaultLogoUrl;

            String cleanKey;
            if (urlToFetch.contains("logos/")) {
                cleanKey = urlToFetch.substring(urlToFetch.indexOf("logos/"));
            } else {
                java.net.URI uri = new java.net.URI(urlToFetch);
                cleanKey = uri.getPath();
                if (cleanKey.startsWith("/")) cleanKey = cleanKey.substring(1);
            }

            String base64Logo = getLogoBase64Cached(cleanKey);
            context.setVariable("logoBase64", base64Logo);

        } catch (Exception e) {
            System.err.println("R2 Error: " + e.getMessage());
            context.setVariable("logoBase64", null);
        }

        if (details.getUpiId() != null && !details.getUpiId().isEmpty() && details.isShowQrOnBill()) {
            String upiUrl = String.format("upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR",
                    details.getUpiId(), details.getName().replace(" ", "%20"), bill.getTotalAmount());
            String qrBase64 = qrCodeGenerator.generateQrCodeBase64(upiUrl, 200, 200);
            context.setVariable("qrCodeBase64", qrBase64);
            context.setVariable("showQr", true);
        } else {
            context.setVariable("showQr", false);
        }
        context.setVariable("taxMode", details.getTaxMode());

        String htmlContent = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            try {
                ITextFontResolver fontResolver = renderer.getFontResolver();
                fontResolver.addFont(resolveFontPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception fontEx) {
                System.err.println("Could not load Unicode font, ₹ symbol may not render: " + fontEx.getMessage());
            }

            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }

    private int computeFillerRows(int itemCount, double totalContentMm, double headerMm,
                                  double tableHeaderMm, double rowMm, double footerMm) {
        double used = headerMm + tableHeaderMm + (itemCount * rowMm) + footerMm;
        double remainingMm = Math.max(0, totalContentMm - used);
        return (int) Math.floor(remainingMm / rowMm);
    }

    private String resolveFontPath() throws IOException {
        String path = cachedFontPath;
        if (path != null) {
            return path;
        }
        synchronized (this) {
            if (cachedFontPath != null) {
                return cachedFontPath;
            }
            ClassPathResource resource = new ClassPathResource("fonts/NotoSans-Regular.ttf");
            try {
                path = resource.getFile().getAbsolutePath();
            } catch (IOException e) {
                Path tempFont = Files.createTempFile("NotoSans-Regular", ".ttf");
                try (InputStream in = resource.getInputStream()) {
                    Files.copy(in, tempFont, StandardCopyOption.REPLACE_EXISTING);
                }
                tempFont.toFile().deleteOnExit();
                path = tempFont.toAbsolutePath().toString();
            }
            cachedFontPath = path;
            return path;
        }
    }

    private String getLogoBase64Cached(String key) {
        CachedLogo cached = logoCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.base64();
        }

        System.out.println("Fetching from R2 - Bucket: " + bucketName + " | Key: " + key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(objectBytes.asByteArray());

        logoCache.put(key, new CachedLogo(base64, Instant.now()));
        return base64;
    }

    private String getIdentificationLabel(String value) {
        if (value == null || value.isEmpty()) return "GSTIN";
        if (value.matches("^[0-9]{12}$")) return "Aadhaar";
        if (value.matches("^[A-Z]{5}[0-9]{4}[A-Z]$")) return "PAN";
        return "GSTIN";
    }

    private String formatAadhaar(String value) {
        if (value != null && value.matches("^[0-9]{12}$")) {
            return value.substring(0, 4) + " " + value.substring(4, 8) + " " + value.substring(8, 12);
        }
        return value;
    }
}