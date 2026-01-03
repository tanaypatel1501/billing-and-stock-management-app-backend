package com.gst.billingandstockmanagement.utils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
public class LogoStorageService {

    @Value("${tebi.bucket}")
    private String bucket;

    @Autowired
    private S3Client s3Client;

    public String uploadLogo(MultipartFile file, Long userId) {

        try {
            String key = "logos/" + userId + "/business-logo.png";

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(file.getBytes())
            );

            return "https://s3.tebi.io/" + bucket + "/" + key;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload logo to Tebi", e);
        }
    }
}

