package com.billingapp.service;

import com.billingapp.entity.Invoice;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;

    public DashboardService(InvoiceRepository invoiceRepository, ClientRepository clientRepository) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Get All Invoices (In a real app, you would filter by UserId here)
        List<Invoice> invoices = invoiceRepository.findAll();

        // 2. Calculate Totals
        double totalRevenue = invoices.stream()
                .mapToDouble(Invoice::getTotal)
                .sum();
                
        double pendingAmount = invoices.stream()
                .filter(inv -> "PENDING".equalsIgnoreCase(inv.getStatus()) || "UNPAID".equalsIgnoreCase(inv.getStatus()))
                .mapToDouble(Invoice::getTotal)
                .sum();

        // 3. Counts
        long totalInvoices = invoices.size();
        long totalClients = clientRepository.count();

        // 4. Get Recent Invoices (Last 5)
        // Sort by IssuedAt (descending) and take top 5
        List<Invoice> recentInvoices = invoices.stream()
                .sorted((i1, i2) -> {
                    if (i1.getIssuedAt() == null || i2.getIssuedAt() == null) return 0;
                    return i2.getIssuedAt().compareTo(i1.getIssuedAt());
                })
                .limit(5)
                .toList();

        // 5. Populate Map
        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingAmount", pendingAmount);
        stats.put("totalInvoices", totalInvoices);
        stats.put("totalClients", totalClients);
        stats.put("recentInvoices", recentInvoices);
        
        // Empty Monthly Stats for now (can be added later)
        stats.put("monthlyStats", List.of());

        return stats;
    }
}