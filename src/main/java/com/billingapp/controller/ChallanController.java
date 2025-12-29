package com.billingapp.controller;

import com.billingapp.entity.Challan;
import com.billingapp.repository.ChallanRepository;
import com.billingapp.service.impl.ChallanPdfServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challans")
public class ChallanController {

    private final ChallanRepository challanRepository;
    private final ChallanPdfServiceImpl pdfService;

    public ChallanController(ChallanRepository challanRepository, ChallanPdfServiceImpl pdfService) {
        this.challanRepository = challanRepository;
        this.pdfService = pdfService;
    }

    @GetMapping
    public List<Challan> getAll() {
        return challanRepository.findAll();
    }

    @PostMapping
    public Challan create(@RequestBody Challan challan) {
        return challanRepository.save(challan);
    }

    // 👇 NEW: Add this missing PUT mapping
    @PutMapping("/{id}")
    public Challan update(@PathVariable String id, @RequestBody Challan challan) {
        // Ensure the ID in the body matches the URL (safety check)
        challan.setId(id);
        return challanRepository.save(challan);
    }

    @GetMapping("/{id}")
    public Challan getById(@PathVariable String id) {
        return challanRepository.findById(id).orElse(null);
    }
    
    // 👇 NEW: Add delete mapping (good to have since you have the button)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        challanRepository.deleteById(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable String id) throws Exception {
        byte[] pdf = pdfService.generateChallanPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=challan_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}