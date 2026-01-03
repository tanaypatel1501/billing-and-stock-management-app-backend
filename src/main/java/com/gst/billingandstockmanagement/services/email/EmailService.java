package com.gst.billingandstockmanagement.services.email;

public interface EmailService {
    void sendEmail(String email, String resetYourPassword, String htmlContent);
}
