package com.billingapp.controller;

import com.billingapp.dto.CreateInvoiceRequest;
import com.billingapp.dto.InvoiceDTO;
import com.billingapp.entity.Invoice;
import com.billingapp.repository.ClientRepository; // 👈 Make sure you have this
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.EmailService;      // 👈 Make sure you have this
import com.billingapp.service.InvoiceService;
import com.billingapp.service.PdfService;        // 👈 Make sure you have this
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired; // Important!
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    // 👇 Inject these 3 new dependencies
    @Autowired
    private PdfService pdfService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private ClientRepository clientRepository;

    // Constructor
    public InvoiceController(InvoiceService invoiceService, InvoiceRepository invoiceRepository) {
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
    }

    // --- Existing Endpoints ---

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> listAll() {
        List<InvoiceDTO> all = invoiceService.getAll();
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getById(@PathVariable String id) {
        try {
            InvoiceDTO dto = invoiceService.getById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<InvoiceDTO> create(@Valid @RequestBody CreateInvoiceRequest req) {
        InvoiceDTO created = invoiceService.create(req);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> update(@PathVariable String id, @Valid @RequestBody CreateInvoiceRequest req) {
        try {
            InvoiceDTO updated = invoiceService.update(id, req);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            invoiceService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<InvoiceDTO>> searchInvoices(
            @RequestParam(value = "clientId", required = false) String clientId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) String fromIso,
            @RequestParam(value = "to", required = false) String toIso,
            @RequestParam(value = "minTotal", required = false) Double minTotal,
            @RequestParam(value = "maxTotal", required = false) Double maxTotal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        Page<InvoiceDTO> result = invoiceService.search(clientId, status, fromIso, toIso, minTotal, maxTotal, page, size, sort);
        return ResponseEntity.ok(result);
    }

    // --- 👇 THE NEW EMAIL ENDPOINT ---

    @PostMapping("/{id}/send-email")
    public ResponseEntity<?> sendInvoiceEmail(@PathVariable String id) {
        try {
            // 1. Get Invoice
            Invoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

            // 2. Get Client Email
            var client = clientRepository.findById(invoice.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("Client not found"));
            
            if (client.getEmail() == null || client.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body("Client does not have an email address.");
            }

            // 3. Generate PDF
            var pdfStream = pdfService.generateInvoicePdf(id);
            byte[] pdfBytes = pdfStream.toByteArray();

            // 4. Send Email
            String subject = "Invoice #" + invoice.getInvoiceNo() + " from Billing App";
            String body = "Dear " + client.getName() + ",\n\nPlease find attached your invoice.\n\nThank you,\nMy Billing Company";
            
            emailService.sendEmailWithAttachment(
                client.getEmail(),
                subject,
                body,
                pdfBytes,
                "invoice-" + invoice.getInvoiceNo() + ".pdf"
            );

            return ResponseEntity.ok("Email sent successfully to " + client.getEmail());

        } catch (Exception e) {
            e.printStackTrace(); // 👈 This prints the error to your Java Console
            return ResponseEntity.internalServerError().body("Failed to send email: " + e.getMessage());
        }
    }
}