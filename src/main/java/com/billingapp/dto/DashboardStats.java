package com.billingapp.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardStats {
    private double totalRevenue;      // Sum of all PAID invoices
    private double pendingAmount;     // Sum of UNPAID/PENDING invoices
    private long totalInvoices;       // Total count
    private long totalClients;        // Total client count
    private List<InvoiceDTO> recentInvoices; // Last 5 invoices
    
    // For the Chart (Last 6 months revenue)
    private List<MonthlyRevenue> monthlyStats;

    @Data
    @Builder
    public static class MonthlyRevenue {
        private String month; // e.g., "Nov 2025"
        private double amount;
    }
}