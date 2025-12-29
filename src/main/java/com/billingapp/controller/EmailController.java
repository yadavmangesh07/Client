package com.billingapp.controller;

import com.billingapp.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/email") // 👈 This maps the URL to /api/email
@CrossOrigin("*")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send-invoice") // 👈 This completes the URL: /api/email/send-invoice
    public ResponseEntity<String> sendInvoiceEmail(
            @RequestParam("invoiceId") String invoiceId,
            @RequestParam("toEmail") String toEmail,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        try {
            emailService.sendInvoiceWithAttachments(invoiceId, toEmail, files);
            return ResponseEntity.ok("Email sent successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }
}