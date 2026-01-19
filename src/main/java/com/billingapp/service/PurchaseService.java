package com.billingapp.service;

import com.billingapp.dto.PurchaseStatsDTO;
import com.billingapp.entity.Purchase;
import com.billingapp.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PurchaseService {

    @Autowired
    private PurchaseRepository repository;

    public List<Purchase> getAll() {
        return repository.findAll();
    }

    public Purchase save(Purchase purchase) {
        calculateStatus(purchase);
        return repository.save(purchase);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    private void calculateStatus(Purchase purchase) {
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
    }

    public PurchaseStatsDTO getMonthlyStats(int month, int year) {
        List<Purchase> all = repository.findAll();
        double total = 0;
        double paid = 0;

        for (Purchase p : all) {
            // 👇 FIXED: No need to parse. Used getter directly.
            if (p.getInvoiceDate() != null) {
                LocalDate date = p.getInvoiceDate(); 
                
                if (date.getMonthValue() == month && date.getYear() == year) {
                    total += (p.getTotalAmount() != null ? p.getTotalAmount().doubleValue() : 0);
                    paid += (p.getAmountPaid() != null ? p.getAmountPaid().doubleValue() : 0);
                }
            }
        }
        return new PurchaseStatsDTO(total, paid, total - paid);
    }

    public PurchaseStatsDTO getYearlyStats(int startYear) {
        List<Purchase> all = repository.findAll();
        double total = 0;
        double paid = 0;

        LocalDate start = LocalDate.of(startYear, 4, 1);
        LocalDate end = LocalDate.of(startYear + 1, 3, 31);

        for (Purchase p : all) {
            // 👇 FIXED: No need to parse. Used getter directly.
            if (p.getInvoiceDate() != null) {
                LocalDate date = p.getInvoiceDate();
                
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    total += (p.getTotalAmount() != null ? p.getTotalAmount().doubleValue() : 0);
                    paid += (p.getAmountPaid() != null ? p.getAmountPaid().doubleValue() : 0);
                }
            }
        }
        return new PurchaseStatsDTO(total, paid, total - paid);
    }
}