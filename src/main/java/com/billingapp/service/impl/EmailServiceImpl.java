package com.billingapp.service.impl;

import com.billingapp.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String filename) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        
        // true = multipart (needed for attachments)
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setFrom("noreply@billingapp.com");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);

        // Add the PDF attachment
        helper.addAttachment(filename, new ByteArrayResource(attachment));

        mailSender.send(message);
        System.out.println("📧 Email sent successfully to: " + to);
    }
}