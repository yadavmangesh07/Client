package com.billingapp.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class InvoiceDTO {
    private String id;
    private String invoiceNo;
    private String clientId;
    private List<InvoiceItemDTO> items;
    private double subtotal;
    private double tax;
    private double total;
    private String status;
    private Instant issuedAt;
    private Instant dueDate;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class InvoiceItemDTO {
        private String description;
        private int qty;
        private double rate;
        private double amount; // computed
    }
}
