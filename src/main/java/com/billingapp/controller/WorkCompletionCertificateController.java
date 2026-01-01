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
@CrossOrigin(origins = "*") // Allow React to access this
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
        // Default values if missing
        if (wcc.getCompanyName() == null) wcc.setCompanyName("JMD DECOR");
        return repository.save(wcc);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkCompletionCertificate> update(@PathVariable String id, @RequestBody WorkCompletionCertificate wcc) {
        return repository.findById(id)
                .map(existing -> {
                    wcc.setId(id);
                    wcc.setCreatedAt(existing.getCreatedAt()); // Preserve creation date
                    return ResponseEntity.ok(repository.save(wcc));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}