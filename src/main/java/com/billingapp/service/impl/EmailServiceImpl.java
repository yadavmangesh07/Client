package com.billingapp.service.impl;

import com.billingapp.entity.Invoice;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.EmailService;
import com.billingapp.service.PdfService; 
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Objects;

@Slf4j
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
        log.info("Initiating dynamic email dispatch sequence with attachments for Invoice ID: {} to recipient: {}", invoiceId, toEmail);
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> {
                        log.error("Email dispatch failed: target Invoice entity reference ID {} non-existent", invoiceId);
                        return new RuntimeException("Invoice not found");
                    });

            log.debug("Generating document byte stream for attachment inclusion matching Invoice No: {}", invoice.getInvoiceNo());
            byte[] pdfBytes = pdfService.generateInvoicePdf(invoiceId);

            Context context = new Context();
            context.setVariable("invoiceNo", invoice.getInvoiceNo());
            
            String htmlContent = templateEngine.process("invoice-mail", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("jmd.decor.billing@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("Invoice #" + invoice.getInvoiceNo() + " - JMD Decor");
            helper.setText(htmlContent, true);

            helper.addAttachment("Invoice_" + invoice.getInvoiceNo() + ".pdf", new ByteArrayResource(pdfBytes));

            if (attachments != null && !attachments.isEmpty()) {
                log.debug("Processing {} additional multi-part file uploads for transmission mapping encapsulation", attachments.size());
                for (MultipartFile file : attachments) {
                    if (!file.isEmpty()) {
                        String fileName = Objects.requireNonNull(file.getOriginalFilename());
                        log.debug("Appending structural runtime multipart attachment link file name: {}", fileName);
                        helper.addAttachment(fileName, file);
                    }
                }
            }

            mailSender.send(message);
            log.info("Email communication transaction successfully committed to SMTP network stream for Invoice No: {}", invoice.getInvoiceNo());

        } catch (Exception e) {
            log.error("SMTP network integration failure transferring payload content packet tracking ID " + invoiceId + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    // --- 2. The Old Method (Required by Interface) ---
    @Override
    public void sendEmailWithAttachment(String to, String subject, String body, byte[] pdfBytes, String fileName) throws Exception {
        log.info("Initiating basic standard email delivery transmission protocol to target node address: {}", to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("jmd.decor.billing@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            
            Context context = new Context();
            context.setVariable("bodyContent", body); 
            String htmlContent = templateEngine.process("invoice-mail", context); 
            
            helper.setText(htmlContent, true);

            if (pdfBytes != null && pdfBytes.length > 0) {
                log.debug("Injecting explicit compiled resource document file array data payload into helper attachment stream: {}", fileName);
                helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));
            }

            mailSender.send(message);
            log.info("Standard communication message packet successfully transmitted out over network link layer to recipient: {}", to);
        } catch (Exception e) {
            log.error("Standard structural mail pipeline engine failure during delivery vector calculation to target " + to + ": " + e.getMessage(), e);
            throw e;
        }
    }
}