package com.billingapp.dto;

public class PurchaseStatsDTO {
    private Double totalExpense;
    private Double totalPaid;
    private Double totalUnpaid;

    public PurchaseStatsDTO(Double totalExpense, Double totalPaid, Double totalUnpaid) {
        this.totalExpense = totalExpense;
        this.totalPaid = totalPaid;
        this.totalUnpaid = totalUnpaid;
    }

    // Getters
    public Double getTotalExpense() { return totalExpense; }
    public Double getTotalPaid() { return totalPaid; }
    public Double getTotalUnpaid() { return totalUnpaid; }
}