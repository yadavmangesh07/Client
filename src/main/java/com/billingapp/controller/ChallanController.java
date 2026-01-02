package com.billingapp.controller;

import com.billingapp.entity.Challan;
import com.billingapp.repository.ChallanRepository;
import com.billingapp.service.impl.ChallanPdfServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/challans")
@CrossOrigin(origins = "*")
public class ChallanController {

    private final ChallanRepository repository;
    private final ChallanPdfServiceImpl pdfService;

    public ChallanController(ChallanRepository repository, ChallanPdfServiceImpl pdfService) {
        this.repository = repository;
        this.pdfService = pdfService;
    }

    @GetMapping
    public List<Challan> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Challan challan) {
        // 1. Set Creation Date
        challan.setCreatedAt(Instant.now());
        challan.setUpdatedAt(Instant.now());

        // 2. Auto-Generate Number if empty OR if it's a duplicate placeholder
        // We run this in a loop to ensure we find a truly unique number
        if (challan.getChallanNo() == null || challan.getChallanNo().trim().isEmpty() || repository.existsByChallanNo(challan.getChallanNo())) {
             String nextNo = generateNextChallanNo();
             
             // Double check: If even the generated one exists (rare race condition), increment until safe
             int attempts = 0;
             while(repository.existsByChallanNo(nextNo) && attempts < 5) {
                 nextNo = incrementChallanNo(nextNo);
                 attempts++;
             }
             challan.setChallanNo(nextNo);
        }

        // 3. Final Safety Check
        if (repository.existsByChallanNo(challan.getChallanNo())) {
            return ResponseEntity.badRequest().body("Error: Challan Number " + challan.getChallanNo() + " already exists. Please try again.");
        }

        return ResponseEntity.ok(repository.save(challan));
    }

    @PutMapping("/{id}")
    public Challan update(@PathVariable String id, @RequestBody Challan req) {
        return repository.findById(id).map(existing -> {
            req.setId(id);
            req.setCreatedAt(existing.getCreatedAt()); // Keep original date
            req.setUpdatedAt(Instant.now());
            
            // Prevent changing Challan No to a duplicate
            if (!existing.getChallanNo().equals(req.getChallanNo()) && repository.existsByChallanNo(req.getChallanNo())) {
                throw new RuntimeException("Challan Number already exists");
            }

            return repository.save(req);
        }).orElseThrow(() -> new RuntimeException("Challan not found"));
    }

    @GetMapping("/{id}")
    public Challan getById(@PathVariable String id) {
        return repository.findById(id).orElse(null);
    }
    
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        repository.deleteById(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable String id) throws Exception {
        byte[] pdf = pdfService.generateChallanPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=challan_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // --- LOGIC TO FIND HIGHEST NUMBER ---
    private String generateNextChallanNo() {
        // 1. Calculate Financial Year
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        int year = today.getYear();
        int month = today.getMonthValue();
        
        String fy;
        if (month >= 4) { // April onwards (e.g., April 2025 is FY 2025-26)
            fy = year + "-" + (year + 1 - 2000);
        } else { // Jan-Mar (e.g., Jan 2026 is still FY 2025-26)
            fy = (year - 1) + "-" + (year - 2000);
        }

        String prefix = "JMD/" + fy + "/";

        // 2. Find ALL existing numbers with this prefix
        List<Challan> existing = repository.findByChallanNoStartingWith(prefix);

        // 3. Find the Maximum Number mathematically
        int maxNum = 0;
        for (Challan c : existing) {
            if (c.getChallanNo() != null) {
                try {
                    String numPart = c.getChallanNo().replace(prefix, "");
                    int num = Integer.parseInt(numPart);
                    if (num > maxNum) {
                        maxNum = num;
                    }
                } catch (Exception ignored) {
                    // Ignore malformed numbers
                }
            }
        }

        // 4. Generate Next
        return prefix + String.format("%03d", maxNum + 1);
    }

    private String incrementChallanNo(String current) {
        try {
            String prefix = current.substring(0, current.lastIndexOf('/') + 1);
            String numPart = current.substring(current.lastIndexOf('/') + 1);
            int next = Integer.parseInt(numPart) + 1;
            return prefix + String.format("%03d", next);
        } catch (Exception e) {
            return current + "-1"; // Fallback
        }
    }
}