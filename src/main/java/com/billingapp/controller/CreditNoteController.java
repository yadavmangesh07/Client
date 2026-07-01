package com.billingapp.controller;

import com.billingapp.entity.CreditNote;
import com.billingapp.service.CreditNoteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit-notes") // 🟢 Maps perfectly to the incoming /api/credit-notes path
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    public CreditNoteController(CreditNoteService creditNoteService) {
        this.creditNoteService = creditNoteService;
    }

    @PostMapping
    public ResponseEntity<CreditNote> create(@RequestBody CreditNote creditNote) {
        return ResponseEntity.ok(creditNoteService.create(creditNote));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreditNote> update(@PathVariable String id, @RequestBody CreditNote creditNote) {
        return ResponseEntity.ok(creditNoteService.update(id, creditNote));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditNote> getById(@PathVariable String id) {
        return ResponseEntity.ok(creditNoteService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<CreditNote>> getAll() {
        return ResponseEntity.ok(creditNoteService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        creditNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) throws Exception {
        byte[] pdfBytes = creditNoteService.generatePdf(id);
        CreditNote cn = creditNoteService.getById(id);
        
        String cleanNo = cn.getCreditNoteNo().replace("/", "-");
        String filename = "CreditNote_" + cleanNo + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}