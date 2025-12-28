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

        // 1. REVENUE (Database Calculation)
        // Handle null in case there are no invoices yet
        Double totalRevenue = invoiceRepository.sumTotalAmount();
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);

        // 2. PENDING AMOUNT (Database Calculation)
        // Using "UNPAID" based on your screenshots. 
        // If you use "PENDING" as well, you can sum both: sumTotalByStatus("UNPAID") + sumTotalByStatus("PENDING")
        Double unpaid = invoiceRepository.sumTotalByStatus("UNPAID");
        stats.put("pendingAmount", unpaid != null ? unpaid : 0.0);
        
        // 3. COUNTS (Efficient DB Count)
        stats.put("totalInvoices", invoiceRepository.count());
        stats.put("totalClients", clientRepository.count());
        
        // Count specific statuses for the UI badges if needed
        stats.put("paidInvoices", invoiceRepository.countByStatus("PAID"));
        stats.put("pendingInvoices", invoiceRepository.countByStatus("UNPAID"));

        // 4. RECENT ACTIVITY (Top 5 only)
        List<Invoice> recentInvoices = invoiceRepository.findTop5ByOrderByCreatedAtDesc();
        stats.put("recentInvoices", recentInvoices);
        
        // 5. Monthly Stats Placeholder (Can implement aggregation later)
        stats.put("monthlyStats", List.of());

        return stats;
    }
}