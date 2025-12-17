package com.billingapp.controller;

import com.billingapp.entity.Attachment;
import com.billingapp.entity.Invoice;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
//import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/attachments")
public class InvoiceAttachmentController {

    private final InvoiceRepository invoiceRepository;
    private final StorageService storage;

    public InvoiceAttachmentController(InvoiceRepository invoiceRepository, StorageService storage) {
        this.invoiceRepository = invoiceRepository;
        this.storage = storage;
    }

    @PostMapping
    public ResponseEntity<?> uploadAttachments(@PathVariable String invoiceId,
                                               @RequestParam("files") MultipartFile[] files) throws Exception {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        for (MultipartFile file : files) {
            String stored = storage.store(file);
            Attachment a = new Attachment();
            a.setFilename(stored);
            a.setOriginalName(file.getOriginalFilename());
            a.setUrl("/files/" + stored);
            a.setContentType(file.getContentType());
            a.setSize(file.getSize());
            a.setUploadedAt(Instant.now());
            invoice.getAttachments().add(a);
        }

        Invoice saved = invoiceRepository.save(invoice);
        List<Attachment> added = saved.getAttachments();
        return ResponseEntity.ok(added);
    }

    @GetMapping
    public ResponseEntity<List<Attachment>> listAttachments(@PathVariable String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        return ResponseEntity.ok(invoice.getAttachments());
    }
}
