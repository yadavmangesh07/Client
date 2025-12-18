package com.billingapp.controller; // Change package if yours is different


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.billingapp.dto.DashboardStats;
import com.billingapp.entity.Invoice;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.service.DashboardService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final DashboardService dashboardService;

    
    @GetMapping
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    public DashboardController(DashboardService dashboardService,ClientRepository clientRepository, InvoiceRepository invoiceRepository) {
        this.clientRepository = clientRepository;
        this.invoiceRepository = invoiceRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Count Clients
        long totalClients = clientRepository.count();

        // 2. Count Invoices
        long totalInvoices = invoiceRepository.count();

        // 3. Calculate Revenue (Sum of invoices with status 'PAID')
        // Note: In a real app, do this via a custom @Query in the Repository for performance.
        // For now, we stream the list (okay for small datasets).
        List<Invoice> allInvoices = invoiceRepository.findAll();
        double totalRevenue = allInvoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                .mapToDouble(Invoice::getTotal)
                .sum();

        stats.put("totalClients", totalClients);
        stats.put("totalInvoices", totalInvoices);
        stats.put("totalRevenue", totalRevenue);

        return stats;
    }
}