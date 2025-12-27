package com.billingapp.service.impl;

import com.billingapp.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // 👈 Inject Template Engine

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] pdfBytes, String fileName) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom("jmd.decor.billing@gmail.com");
        helper.setTo(to);
        helper.setSubject(subject);
        
        // 👇 1. Prepare Data for the Template
        Context context = new Context();
        context.setVariable("bodyContent", body); // Pass the dynamic body text
        
        // 👇 2. Process the HTML Template
        // This looks for 'invoice-mail.html' in src/main/resources/templates/
        String htmlContent = templateEngine.process("invoice-mail", context);
        
        helper.setText(htmlContent, true);

        if (pdfBytes != null && pdfBytes.length > 0) {
            helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));
        }

        mailSender.send(message);
        System.out.println("📧 Template Email sent to: " + to);
    }
}