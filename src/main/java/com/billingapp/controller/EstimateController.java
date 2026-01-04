package com.billingapp.controller;

import com.billingapp.entity.Estimate;
import com.billingapp.service.EstimateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estimates")
@CrossOrigin("*") // Allow frontend to access this
public class EstimateController {

    private final EstimateService estimateService;

    public EstimateController(EstimateService estimateService) {
        this.estimateService = estimateService;
    }

    // 1. Get All Estimates
    @GetMapping
    public List<Estimate> getAll() {
        return estimateService.getAllEstimates();
    }

    // 2. Create New Estimate
    @PostMapping
    public Estimate create(@RequestBody Estimate estimate) {
        return estimateService.createEstimate(estimate);
    }

    // 3. Update Existing Estimate
    @PutMapping("/{id}")
    public Estimate update(@PathVariable String id, @RequestBody Estimate estimate) {
        return estimateService.updateEstimate(id, estimate);
    }

    // 4. Delete Estimate
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        estimateService.deleteEstimate(id);
    }

    // 5. Download PDF
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) {
        try {
            byte[] pdf = estimateService.generateEstimatePdf(id);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=estimate_" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            e.printStackTrace(); // Print error to console for debugging
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<Estimate> getById(@PathVariable String id) {
        try {
            Estimate estimate = estimateService.getEstimateById(id);
            return ResponseEntity.ok(estimate);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}