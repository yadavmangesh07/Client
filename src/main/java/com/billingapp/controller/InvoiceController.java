package com.billingapp.controller;

import com.billingapp.dto.CreateInvoiceRequest;
import com.billingapp.dto.InvoiceDTO;
import com.billingapp.entity.Client;
import com.billingapp.entity.Invoice;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.EmailService;
import com.billingapp.service.EwayBillService;
import com.billingapp.service.InvoiceService;
import com.billingapp.service.PdfService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin("*") // Ensures frontend can access this
public class InvoiceController {

    // 1. Declare ALL dependencies as final
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final EwayBillService ewayBillService;

    // 2. Single Constructor for Injection (Best Practice)
    public InvoiceController(InvoiceService invoiceService,
                             InvoiceRepository invoiceRepository,
                             ClientRepository clientRepository,
                             PdfService pdfService,
                             EmailService emailService,
                             EwayBillService ewayBillService) {
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.ewayBillService = ewayBillService;
    }

    // --- Endpoints ---

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> listAll() {
        return ResponseEntity.ok(invoiceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(invoiceService.getById(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<InvoiceDTO> create(@Valid @RequestBody CreateInvoiceRequest req) {
        return ResponseEntity.ok(invoiceService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceDTO> update(@PathVariable String id, @Valid @RequestBody CreateInvoiceRequest req) {
        try {
            return ResponseEntity.ok(invoiceService.update(id, req));
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
        return ResponseEntity.ok(invoiceService.search(clientId, status, fromIso, toIso, minTotal, maxTotal, page, size, sort));
    }

    // --- EMAIL ENDPOINT (Quick Send) ---
    // Note: The new Popup uses EmailController, but we keep this as a backup API
    @PostMapping("/{id}/send-email")
    public ResponseEntity<?> sendInvoiceEmail(@PathVariable String id) {
        try {
            Invoice invoice = invoiceRepository.findById(id).orElseThrow();
            Client client = clientRepository.findById(invoice.getClientId()).orElseThrow();
            
            byte[] pdfBytes = pdfService.generateInvoicePdf(id);
            
            String body = "We hope you are doing well. Here is the invoice <b>#" + invoice.getInvoiceNo() + "</b> for your recent order.";
            
            emailService.sendEmailWithAttachment(
                client.getEmail(),
                "Invoice #" + invoice.getInvoiceNo() + " from JMD Decor",
                body,
                pdfBytes,
                "Invoice_" + invoice.getInvoiceNo() + ".pdf"
            );
            
            return ResponseEntity.ok("Email sent successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error sending email: " + e.getMessage());
        }
    }

    // --- E-WAY BILL ENDPOINTS ---

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
            
            invoice.setEwayBillNo(ewayBillNo);
            invoiceRepository.save(invoice);
            
            return ResponseEntity.ok("E-Way Bill Number updated");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update");
        }
    }
}