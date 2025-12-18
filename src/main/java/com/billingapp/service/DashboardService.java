package com.billingapp.service;

import com.billingapp.dto.DashboardStats;
import com.billingapp.dto.InvoiceDTO;
import com.billingapp.entity.Invoice;
import com.billingapp.mapper.InvoiceMapper;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.InvoiceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final InvoiceMapper invoiceMapper;

    public DashboardService(InvoiceRepository invoiceRepository, ClientRepository clientRepository, InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.invoiceMapper = invoiceMapper;
    }

    public DashboardStats getStats() {
        List<Invoice> allInvoices = invoiceRepository.findAll();

        // 1. Calculate Totals
        double revenue = allInvoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                .mapToDouble(Invoice::getTotal)
                .sum();

        double pending = allInvoices.stream()
                .filter(i -> !"PAID".equalsIgnoreCase(i.getStatus()) && !"DRAFT".equalsIgnoreCase(i.getStatus()))
                .mapToDouble(Invoice::getTotal)
                .sum();

        long totalClients = clientRepository.count();

        // 2. Get Recent Invoices (Last 5)
        List<Invoice> recent = invoiceRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "issuedAt"))
        ).getContent();
        List<InvoiceDTO> recentDTOs = recent.stream().map(invoiceMapper::toDto).collect(Collectors.toList());

        // 3. Calculate Monthly Stats (Simple version for last 6 months)
        List<DashboardStats.MonthlyRevenue> monthlyStats = calculateMonthlyStats(allInvoices);

        return DashboardStats.builder()
                .totalRevenue(revenue)
                .pendingAmount(pending)
                .totalInvoices(allInvoices.size())
                .totalClients(totalClients)
                .recentInvoices(recentDTOs)
                .monthlyStats(monthlyStats)
                .build();
    }

    private List<DashboardStats.MonthlyRevenue> calculateMonthlyStats(List<Invoice> invoices) {
        // Group revenue by Month (Last 6 months)
        List<DashboardStats.MonthlyRevenue> stats = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth targetMonth = current.minusMonths(i);
            String label = targetMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            
            double monthTotal = invoices.stream()
                    .filter(inv -> "PAID".equalsIgnoreCase(inv.getStatus()))
                    .filter(inv -> {
                        if (inv.getIssuedAt() == null) return false;
                        LocalDate date = inv.getIssuedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                        return YearMonth.from(date).equals(targetMonth);
                    })
                    .mapToDouble(Invoice::getTotal)
                    .sum();

            stats.add(DashboardStats.MonthlyRevenue.builder().month(label).amount(monthTotal).build());
        }
        return stats;
    }
}