package com.billingapp.controller;

import com.billingapp.dto.CreateInvoiceRequest;
import com.billingapp.dto.InvoiceDTO;
import com.billingapp.service.InvoiceService;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

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
    public ResponseEntity<InvoiceDTO> update(@PathVariable String id,
                                             @Valid @RequestBody CreateInvoiceRequest req) {
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
}
