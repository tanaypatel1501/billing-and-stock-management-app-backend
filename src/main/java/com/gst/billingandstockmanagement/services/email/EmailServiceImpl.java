package com.gst.billingandstockmanagement.services.email;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private Gmail gmail;

    @Value("${gmail.from-email}")
    private String fromEmail;

    @Async
    @Override
    public void sendEmail(String to, String subject, String htmlContent) {

        try {
            Properties props = new Properties();
            Session session = Session.getInstance(props, null);

            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress(fromEmail));
            email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
            email.setSubject(subject);
            email.setContent(htmlContent, "text/html; charset=UTF-8");

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);

            String encodedEmail = Base64.getUrlEncoder()
                    .encodeToString(buffer.toByteArray());

            Message message = new Message();
            message.setRaw(encodedEmail);

            gmail.users().messages().send("me", message).execute();

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email via Gmail API", e);
        }
    }
}