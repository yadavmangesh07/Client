package com.billingapp.controller;

import com.billingapp.dto.CreateInvoiceRequest;
import com.billingapp.dto.InvoiceDTO;
import com.billingapp.entity.Client;
import com.billingapp.entity.Invoice;
import com.billingapp.repository.ClientRepository; // 👈 Make sure you have this
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.EmailService;      // 👈 Make sure you have this
import com.billingapp.service.EwayBillService;
import com.billingapp.service.InvoiceService;
import com.billingapp.service.PdfService;        // 👈 Make sure you have this
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired; // Important!
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
            // 1. Fetch data needed for Email Subject/Body
            Invoice invoice = invoiceRepository.findById(id).orElseThrow();
            Client client = clientRepository.findById(invoice.getClientId()).orElseThrow();
            
            // 2. Generate PDF (Pass ID, not the object)
            byte[] pdfBytes = pdfService.generateInvoicePdf(id); // 👈 FIXED HERE
            
            // 3. Prepare Email
            String body = "We hope you are doing well. Here is the invoice <b>#" + invoice.getId().substring(0,8) + "</b> for your recent order.";
            
            emailService.sendEmailWithAttachment(
                client.getEmail(),
                "Invoice #" + invoice.getId().substring(0,6) + " from JMD Decor",
                body,
                pdfBytes,
                "Invoice_" + invoice.getId().substring(0,6) + ".pdf"
            );
            
            return ResponseEntity.ok("Email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error sending email: " + e.getMessage());
        }
    }

    // ... other imports
    @Autowired private EwayBillService ewayBillService; // Inject this

    @GetMapping("/{id}/eway-json")
    public ResponseEntity<byte[]> downloadEwayJson(@PathVariable String id) {
        try {
            byte[] jsonBytes = ewayBillService.generateJson(id);
            String filename = "ewaybill_" + id + ".json";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    @PatchMapping("/{id}/eway-bill")
    public ResponseEntity<?> updateEwayBill(@PathVariable String id, @RequestBody Map<String, String> payload) {
        try {
            String ewayBillNo = payload.get("ewayBillNo");
            Invoice invoice = invoiceRepository.findById(id).orElseThrow();
            
            invoice.setEwayBillNo(ewayBillNo); // Update field
            invoiceRepository.save(invoice);
            
            return ResponseEntity.ok("E-Way Bill Number updated");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update");
        }
    }
}