package com.billingapp.service;

import com.billingapp.entity.Invoice;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;

    public DashboardService(InvoiceRepository invoiceRepository, ClientRepository clientRepository) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
    }

    public Map<String, Object> getDashboardStats() {
        // 1. Define all tasks to run in PARALLEL
        
        // Task A: Total Revenue
        CompletableFuture<Double> revenueTask = CompletableFuture.supplyAsync(() -> 
            invoiceRepository.sumTotalAmount()
        );

        // Task B: Unpaid Amount
        CompletableFuture<Double> unpaidTask = CompletableFuture.supplyAsync(() -> 
            invoiceRepository.sumTotalByStatus("UNPAID")
        );
        
        // Task C: Pending Amount
        CompletableFuture<Double> pendingTask = CompletableFuture.supplyAsync(() -> 
            invoiceRepository.sumTotalByStatus("PENDING")
        );

        // Task D: Invoice Count
        CompletableFuture<Long> invoiceCountTask = CompletableFuture.supplyAsync(() -> 
            invoiceRepository.count()
        );

        // Task E: Client Count
        CompletableFuture<Long> clientCountTask = CompletableFuture.supplyAsync(() -> 
            clientRepository.count()
        );

        // Task F: Recent Invoices (Top 5)
        CompletableFuture<List<Invoice>> recentTask = CompletableFuture.supplyAsync(() -> 
            invoiceRepository.findTop5ByOrderByCreatedAtDesc()
        );

        // Task G: Chart Data (Fetch All)
        // Note: For massive scale (10k+), we should move this logic to MongoDB Aggregation
        CompletableFuture<List<Invoice>> allInvoicesTask = CompletableFuture.supplyAsync(() -> 
            invoiceRepository.findAll()
        );

        // 2. Wait for all tasks to finish
        CompletableFuture.allOf(
            revenueTask, unpaidTask, pendingTask, invoiceCountTask, 
            clientCountTask, recentTask, allInvoicesTask
        ).join();

        // 3. Assemble the Response
        Map<String, Object> stats = new HashMap<>();

        try {
            Double revenue = revenueTask.get();
            stats.put("totalRevenue", revenue != null ? revenue : 0.0);

            Double unpaid = unpaidTask.get();
            Double pending = pendingTask.get();
            stats.put("pendingAmount", (unpaid != null ? unpaid : 0.0) + (pending != null ? pending : 0.0));

            stats.put("totalInvoices", invoiceCountTask.get());
            stats.put("totalClients", clientCountTask.get());
            stats.put("recentInvoices", recentTask.get());
            stats.put("monthlyStats", calculateMonthlyStats(allInvoicesTask.get()));
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching dashboard stats");
        }

        return stats;
    }

    // --- Helper Method (Same as before) ---
    private List<Map<String, Object>> calculateMonthlyStats(List<Invoice> invoices) {
        Map<String, Double> tempMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");

        invoices.sort(Comparator.comparing(Invoice::getIssuedAt));

        for (Invoice inv : invoices) {
            if (inv.getIssuedAt() != null) {
                try {
                    LocalDate date = inv.getIssuedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                    String monthName = date.format(formatter);
                    tempMap.put(monthName, tempMap.getOrDefault(monthName, 0.0) + inv.getTotal());
                } catch (Exception ignored) {}
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : tempMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", entry.getKey());
            item.put("amount", entry.getValue());
            result.add(item);
        }
        return result;
    }
}