package com.billingapp.controller;

import com.billingapp.entity.Challan;
import com.billingapp.repository.ChallanRepository;
import com.billingapp.service.impl.ChallanPdfServiceImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
        // 1. Audit Fields
        challan.setCreatedAt(Instant.now());
        challan.setUpdatedAt(Instant.now());

        // 🟢 MANUAL CHANGE: Validate that a manual challan number was provided
        if (challan.getChallanNo() == null || challan.getChallanNo().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Challan Number is required for manual entry.");
        }

        // 🟢 MANUAL CHANGE: Check for uniqueness to prevent duplicates
        if (repository.existsByChallanNo(challan.getChallanNo())) {
            return ResponseEntity.badRequest().body("Error: Challan Number " + challan.getChallanNo() + " already exists.");
        }

        /* 👇 COMMENTED OUT: Auto-Generation Logic
        if (challan.getChallanNo() == null || challan.getChallanNo().trim().isEmpty() || repository.existsByChallanNo(challan.getChallanNo())) {
             String nextNo = generateNextChallanNo();
             ...
             challan.setChallanNo(nextNo);
        }
        */

        return ResponseEntity.ok(repository.save(challan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Challan req) {
        return repository.findById(id).map(existing -> {
            req.setId(id);
            req.setCreatedAt(existing.getCreatedAt()); // Keep original date
            req.setUpdatedAt(Instant.now());
            
            // 🟢 MANUAL CHANGE: Prevent changing Challan No to a duplicate that belongs to another record
            if (!existing.getChallanNo().equals(req.getChallanNo()) && repository.existsByChallanNo(req.getChallanNo())) {
                return ResponseEntity.badRequest().body("Error: Challan Number already exists.");
            }

            return ResponseEntity.ok(repository.save(req));
        }).orElse(ResponseEntity.notFound().build());
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

    /* 👇 COMMENTED OUT: Auto-Generation Logic Methods
    
    private String generateNextChallanNo() {
        ...
    }

    private String incrementChallanNo(String current) {
        ...
    }
    */
}