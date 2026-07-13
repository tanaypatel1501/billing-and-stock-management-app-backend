package com.gst.billingandstockmanagement.services.pdf;

import com.gst.billingandstockmanagement.entities.Bill;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // ── Page-fill spacer estimates (mm). Print a 1-item and a 10-item bill,
    // measure any gap/overflow, and nudge these. Row height shifts a bit if
    // a product name wraps to two lines.
    private static final double TOTAL_CONTENT_MM = 197.0;
    private static final double TITLE_MM = 12.0;
    private static final double TOP_SECTION_MM = 58.0;
    private static final double TABLE_HEADER_MM = 8.0;
    private static final double ROW_MM = 8.0;
    private static final double FOOTER_MM = 32.0;

    // ── In-memory logo cache: avoids hitting S3 on every single PDF render.
    private static final Duration LOGO_CACHE_TTL = Duration.ofMinutes(30);
    private final Map<String, CachedLogo> logoCache = new ConcurrentHashMap<>();

    private record CachedLogo(String base64, Instant fetchedAt) {
        boolean isExpired() {
            return Duration.between(fetchedAt, Instant.now()).compareTo(LOGO_CACHE_TTL) > 0;
        }
    }

    // ── Resolved once and reused: avoids re-extracting/re-locating the font
    // file on every single PDF render.
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

        // ── Page-fill spacer: pushes the footer to the bottom of the page
        // when there are only 1-2 items, so the invoice doesn't look half-empty.
        int itemCount = bill.getBillItems() != null ? bill.getBillItems().size() : 0;
        double used = TITLE_MM + TOP_SECTION_MM + TABLE_HEADER_MM
                + (itemCount * ROW_MM) + FOOTER_MM;
        double remainingMm = Math.max(0, TOTAL_CONTENT_MM - used);
        int fillerRowCount = (int) Math.floor(remainingMm / ROW_MM);
        context.setVariable("fillerRows", fillerRowCount > 0
                ? java.util.stream.IntStream.range(0, fillerRowCount).boxed().toList()
                : java.util.Collections.emptyList());

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

        String templateName = "template1".equals(details.getPreferredTemplate())
                ? "invoice-template"
                : "invoice-template-2";
        String htmlContent = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            // ── Embed a Unicode font so the ₹ glyph (U+20B9) actually renders.
            // If this fails, log it but don't kill the whole PDF — better to
            // ship an invoice missing ₹ than no invoice at all.
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

    /**
     * Resolves an on-disk path to the embedded font, caching it after first use.
     * Works both when running exploded (IDE/dev) via ClassPathResource#getFile,
     * and when running from a packaged jar (OCI VM deployment) — in that case
     * getFile() throws because the resource lives inside the jar, so we fall
     * back to extracting it once to a temp file.
     */
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

    /**
     * Fetches a logo from S3, caching the base64 result in memory for
     * LOGO_CACHE_TTL. Cuts a network round-trip on every bill's PDF render
     * for logos that essentially never change between requests.
     */
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