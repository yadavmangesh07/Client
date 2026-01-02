package com.billingapp.controller;

import com.billingapp.entity.WorkCompletionCertificate;
import com.billingapp.repository.WorkCompletionCertificateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

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
    public WorkCompletionCertificate create(@RequestBody WorkCompletionCertificate wcc) {
        wcc.setCreatedAt(Instant.now());
        
        // 👇 AUTO-GENERATE LOGIC
        if (wcc.getRefNo() == null || wcc.getRefNo().trim().isEmpty()) {
            wcc.setRefNo(generateNextRefNo());
        }

        if (wcc.getCompanyName() == null) wcc.setCompanyName("JMD DECOR");
        
        return repository.save(wcc);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkCompletionCertificate> update(@PathVariable String id, @RequestBody WorkCompletionCertificate wcc) {
        return repository.findById(id)
                .map(existing -> {
                    wcc.setId(id);
                    wcc.setCreatedAt(existing.getCreatedAt()); 
                    // Preserve existing RefNo if the user accidentally sends empty
                    if (wcc.getRefNo() == null || wcc.getRefNo().isEmpty()) {
                        wcc.setRefNo(existing.getRefNo());
                    }
                    return ResponseEntity.ok(repository.save(wcc));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- HELPER METHODS ---

    private String generateNextRefNo() {
        // 1. Calculate Financial Year (e.g., 2024-25)
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        int year = today.getYear();
        int month = today.getMonthValue();
        
        String fy;
        if (month >= 4) { // April onwards is current year - next year
            fy = year + "-" + (year + 1 - 2000);
        } else { // Jan-Mar is previous year - current year
            fy = (year - 1) + "-" + (year - 2000);
        }

        // Prefix Pattern: JMD/2024-25/
        String prefix = "JMD/" + fy + "/";

        // 2. Find the last certificate
        var lastCertOpt = repository.findTopByOrderByCreatedAtDesc();
        
        int nextNum = 1;
        
        if (lastCertOpt.isPresent()) {
            String lastRef = lastCertOpt.get().getRefNo();
            // Check if the last ref matches current FY pattern
            if (lastRef != null && lastRef.startsWith(prefix)) {
                try {
                    // Extract the number part (e.g., "86" from "JMD/2024-25/86")
                    String numPart = lastRef.substring(prefix.length());
                    nextNum = Integer.parseInt(numPart) + 1;
                } catch (NumberFormatException e) {
                    // If parsing fails, start from 1 safely
                    nextNum = 1;
                }
            }
        }

        // 3. Return combined string
        return prefix + nextNum;
    }
}