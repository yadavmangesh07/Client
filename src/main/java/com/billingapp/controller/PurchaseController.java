package com.billingapp.controller;

import com.billingapp.entity.Purchase;
import com.billingapp.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

//import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*") // Allow frontend access
public class PurchaseController {

    @Autowired
    private PurchaseRepository repository;

    @GetMapping
    public List<Purchase> getAllPurchases() {
        return repository.findAll();
    }

    @PostMapping
    public Purchase createPurchase(@RequestBody Purchase purchase) {
        // Simple logic to auto-set status if not provided
        if (purchase.getStatus() == null) {
            if (purchase.getAmountPaid() != null && purchase.getTotalAmount() != null) {
                if (purchase.getAmountPaid().compareTo(purchase.getTotalAmount()) >= 0) {
                    purchase.setStatus("PAID");
                } else if (purchase.getAmountPaid().doubleValue() > 0) {
                    purchase.setStatus("PARTIAL");
                } else {
                    purchase.setStatus("UNPAID");
                }
            }
        }
        return repository.save(purchase);
    }

    @DeleteMapping("/{id}")
    public void deletePurchase(@PathVariable String id) {
        repository.deleteById(id);
    }
    // 👇 ADD THIS METHOD to handle Updates
    @PutMapping("/{id}")
    public Purchase updatePurchase(@PathVariable String id, @RequestBody Purchase purchase) {
        // 1. Ensure the ID from the URL is set on the object so MongoDB updates it (instead of creating new)
        purchase.setId(id); 

        // 2. (Optional) Re-validate Status on server side just to be safe
        if (purchase.getAmountPaid() != null && purchase.getTotalAmount() != null) {
             double paid = purchase.getAmountPaid().doubleValue();
             double total = purchase.getTotalAmount().doubleValue();

             if (paid >= total && total > 0) {
                 purchase.setStatus("Full Paid");
             } else if (paid > 0) {
                 purchase.setStatus("Partially Paid");
             } else {
                 purchase.setStatus("Unpaid");
             }
        }

        return repository.save(purchase);
    }
}