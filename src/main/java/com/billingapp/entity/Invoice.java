package com.billingapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "invoices")
public class Invoice {

    @Id
    private String id;

    private String invoiceNo;
    private String clientId;
    private List<InvoiceItem> items;
    private double subtotal;
    private double tax;
    private double total;
    private String status;
    private Instant issuedAt;
    private Instant dueDate;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    // 👇 NEW: Address Snapshot (Stored on Invoice)
    private String billingAddress;
    private String shippingAddress;

    // 👇 NEW: Transport Details
    private String ewayBillNo;
    private String transportMode; // e.g. Road

    // 👇 NEW: Order References
    private String challanNo;
    private Instant challanDate;
    private String poNumber;      // Purchase Order No
    private Instant poDate;

    // attachments
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceItem {
        private String description;
        private int qty;
        private double rate;
        private String hsnCode; 
        private String uom;      // NOS, KGS, MTR
        private double taxRate;  // e.g. 18.0

        public double getAmount() {
            return qty * rate;
        }
    }
}
