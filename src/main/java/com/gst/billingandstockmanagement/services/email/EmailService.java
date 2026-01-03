package com.gst.billingandstockmanagement.services.email;

import org.springframework.mail.javamail.MimeMessageHelper;;

public interface EmailService {
    void sendEmail(String email, String resetYourPassword, String htmlContent);
}
