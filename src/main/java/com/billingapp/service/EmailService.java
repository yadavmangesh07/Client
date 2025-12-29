package com.billingapp.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface EmailService {
    // Existing simple method (keep this to satisfy the contract)
    void sendEmailWithAttachment(String to, String subject, String body, byte[] pdfBytes, String fileName) throws Exception;

    // 👇 ADD THIS NEW METHOD
    void sendInvoiceWithAttachments(String invoiceId, String toEmail, List<MultipartFile> attachments);
}