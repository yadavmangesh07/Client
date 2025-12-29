package com.billingapp.service.impl;

import com.billingapp.entity.Invoice;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.EmailService;
import com.billingapp.service.PdfService; 
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Objects;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final PdfService pdfService;
    private final InvoiceRepository invoiceRepository;

    public EmailServiceImpl(JavaMailSender mailSender, 
                            TemplateEngine templateEngine,
                            PdfService pdfService, 
                            InvoiceRepository invoiceRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.pdfService = pdfService;
        this.invoiceRepository = invoiceRepository;
    }

    // --- 1. The New Method (For "Compose Email" Dialog) ---
    @Override
    public void sendInvoiceWithAttachments(String invoiceId, String toEmail, List<MultipartFile> attachments) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            byte[] pdfBytes = pdfService.generateInvoicePdf(invoiceId);

            Context context = new Context();
            context.setVariable("invoiceNo", invoice.getInvoiceNo());
            // context.setVariable("bodyContent", "Please find attached..."); // Optional dynamic body
            
            String htmlContent = templateEngine.process("invoice-mail", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("jmd.decor.billing@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("Invoice #" + invoice.getInvoiceNo() + " - JMD Decor");
            helper.setText(htmlContent, true);

            helper.addAttachment("Invoice_" + invoice.getInvoiceNo() + ".pdf", new ByteArrayResource(pdfBytes));

            if (attachments != null && !attachments.isEmpty()) {
                for (MultipartFile file : attachments) {
                    if (!file.isEmpty()) {
                        helper.addAttachment(Objects.requireNonNull(file.getOriginalFilename()), file);
                    }
                }
            }

            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    // --- 2. The Old Method (Required by Interface) ---
    @Override
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] pdfBytes, String fileName) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom("jmd.decor.billing@gmail.com");
        helper.setTo(to);
        helper.setSubject(subject);
        
        Context context = new Context();
        context.setVariable("bodyContent", body); // Fallback for simple emails
        // Using the same template, or you can create a generic one
        String htmlContent = templateEngine.process("invoice-mail", context); 
        
        helper.setText(htmlContent, true);

        if (pdfBytes != null && pdfBytes.length > 0) {
            helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));
        }

        mailSender.send(message);
    }
}