package com.billingapp.controller;

import com.billingapp.dto.PurchaseStatsDTO;
import com.billingapp.entity.Purchase;
import com.billingapp.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*") 
public class PurchaseController {

    @Autowired
    private PurchaseService service;

    @GetMapping
    public List<Purchase> getAllPurchases() {
        return service.getAll();
    }

    @PostMapping
    public Purchase createPurchase(@RequestBody Purchase purchase) {
        return service.save(purchase);
    }

    @PutMapping("/{id}")
    public Purchase updatePurchase(@PathVariable String id, @RequestBody Purchase purchase) {
        purchase.setId(id);
        return service.save(purchase);
    }

    @DeleteMapping("/{id}")
    public void deletePurchase(@PathVariable String id) {
        service.delete(id);
    }

    // 👇 NEW ENDPOINT: Get Dashboard Stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, PurchaseStatsDTO>> getStats(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        // Default to current date if not provided
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year != null) ? year : now.getYear();

        // Calculate Financial Year Start (e.g., if today is Jan 2026, FY started in 2025)
        int fyStart = (now.getMonthValue() >= 4) ? now.getYear() : now.getYear() - 1;

        PurchaseStatsDTO monthly = service.getMonthlyStats(m, y);
        PurchaseStatsDTO yearly = service.getYearlyStats(fyStart);

        Map<String, PurchaseStatsDTO> response = new HashMap<>();
        response.put("monthly", monthly);
        response.put("yearly", yearly);

        return ResponseEntity.ok(response);
    }
}