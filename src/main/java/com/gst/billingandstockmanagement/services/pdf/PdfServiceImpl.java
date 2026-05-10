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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
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

    @Override
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

        try {
            String urlToFetch = (details.getLogoUrl() != null && !details.getLogoUrl().isEmpty())
                    ? details.getLogoUrl()
                    : defaultLogoUrl;

            String cleanKey;
            // This logic ensures we only grab the part of the URL that is actually the S3 Key
            if (urlToFetch.contains("logos/")) {
                cleanKey = urlToFetch.substring(urlToFetch.indexOf("logos/"));
            } else {
                java.net.URI uri = new java.net.URI(urlToFetch);
                cleanKey = uri.getPath();
                if (cleanKey.startsWith("/")) cleanKey = cleanKey.substring(1);
            }

            System.out.println("Fetching from R2 - Bucket: " + bucketName + " | Key: " + cleanKey);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanKey)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);

            String base64Logo = "data:image/png;base64," + Base64.getEncoder().encodeToString(objectBytes.asByteArray());
            context.setVariable("logoBase64", base64Logo);

        } catch (Exception e) {
            System.err.println("R2 Error: " + e.getMessage());
            // Fallback to null so the PDF still generates without the logo
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
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
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