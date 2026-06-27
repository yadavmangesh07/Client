package com.billingapp.controller;

import com.billingapp.entity.WorkCompletionCertificate;
import com.billingapp.repository.WorkCompletionCertificateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/wcc")
@CrossOrigin(origins = "*")
public class WorkCompletionCertificateController {

    @Autowired
    private WorkCompletionCertificateRepository repository;

    @GetMapping
    public List<WorkCompletionCertificate> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkCompletionCertificate> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WorkCompletionCertificate wcc) {
        wcc.setCreatedAt(Instant.now());
        
        // 🟢 MANUAL CHANGE: Validate that a manual reference number was provided
        if (wcc.getRefNo() == null || wcc.getRefNo().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Reference Number is required for manual entry.");
        }

        // 🟢 MANUAL CHANGE: Check for uniqueness to prevent duplicates
        if (repository.existsByRefNo(wcc.getRefNo())) {
            return ResponseEntity.badRequest().body("Error: Reference Number " + wcc.getRefNo() + " already exists.");
        }

        /* 👇 COMMENTED OUT: Auto-generation logic
        if (wcc.getRefNo() == null || wcc.getRefNo().trim().isEmpty()) {
            wcc.setRefNo(generateNextRefNo());
        }
        */

        if (wcc.getCompanyName() == null) wcc.setCompanyName("JMD DECOR");
        
        return ResponseEntity.ok(repository.save(wcc));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody WorkCompletionCertificate wcc) {
        return repository.findById(id)
                .map(existing -> {
                    wcc.setId(id);
                    wcc.setCreatedAt(existing.getCreatedAt()); 
                    
                    // 🟢 MANUAL CHANGE: Prevent changing Ref No to a duplicate belonging to another document
                    if (wcc.getRefNo() == null || wcc.getRefNo().trim().isEmpty()) {
                        return ResponseEntity.badRequest().body("Error: Reference Number cannot be empty.");
                    }
                    
                    if (!existing.getRefNo().equals(wcc.getRefNo()) && repository.existsByRefNo(wcc.getRefNo())) {
                        return ResponseEntity.badRequest().body("Error: Reference Number already exists.");
                    }

                    // 🟢 Business values like gstin pass directly through the payload object safely
                    return ResponseEntity.ok(repository.save(wcc));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* 👇 COMMENTED OUT: Auto-Generation Logic Helper
    private String generateNextRefNo() {
        ...
    }
    */
}