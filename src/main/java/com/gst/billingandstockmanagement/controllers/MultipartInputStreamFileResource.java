package com.gst.billingandstockmanagement.controllers;

import org.springframework.core.io.InputStreamResource;
import java.io.InputStream;

/**
 * Needed to send a MultipartFile to another service via RestTemplate.
 * Spring's RestTemplate needs a named resource to build multipart form data.
 */
public class MultipartInputStreamFileResource extends InputStreamResource {

    private final String filename;

    public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
        super(inputStream);
        this.filename = filename != null ? filename : "image.jpg";
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long contentLength() {
        return -1; // unknown length — let Spring stream it
    }
}