package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.entities.Details;
import com.gst.billingandstockmanagement.repository.DetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class LogoController {

    @Autowired
    private S3Client s3Client;

    @Value("${tebi.bucket}")
    private String bucketName;

    @GetMapping("/logo/{userId}")
    public ResponseEntity<byte[]> getLogo(@PathVariable Long userId,
                                          DetailsRepository detailsRepository) {
        try {
            Details details = detailsRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Details not found"));

            String logoUrl = details.getLogoUrl();
            if (logoUrl == null || logoUrl.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String cleanKey;
            if (logoUrl.contains("logos/")) {
                cleanKey = logoUrl.substring(logoUrl.indexOf("logos/"));
            } else {
                java.net.URI uri = new java.net.URI(logoUrl);
                cleanKey = uri.getPath();
                if (cleanKey.startsWith("/")) cleanKey = cleanKey.substring(1);
            }

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanKey)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
            String contentType = objectBytes.response().contentType();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            contentType != null ? contentType : "image/png"))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                    .body(objectBytes.asByteArray());

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}