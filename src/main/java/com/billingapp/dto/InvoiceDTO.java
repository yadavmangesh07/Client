package com.billingapp.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class InvoiceDTO {
    private String id;
    private String invoiceNo;
    private String clientId;

    // 👇 NEW: Address Snapshot
    private String billingAddress;
    private String shippingAddress;

    // 👇 NEW: Transport Details
    private String ewayBillNo;
    private String transportMode;

    // 👇 NEW: Order References
    private String challanNo;
    private Instant challanDate;
    private String poNumber;
    private Instant poDate;

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
        
        // 👇 NEW: Item Specifics
        private String hsnCode;
        private String uom;      // Unit of Measurement
        private double taxRate;
        
        private int qty;
        private double rate;
        private double amount; // computed
    }
}