package com.billingapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_notes")
public class CreditNote {
    @Id
    private String id;
    private String creditNoteNo;       // e.g., JMD/2025-26/02
    private Instant creditNoteDate;    // Date: 20-05-2025
    
    // Status Track Context Flag
    private String status;             // 🟢 Added: "DRAFT" or "COMPLETED" flag mapping
    
    // References to Original Document
    private String billReferenceNo;    // Bill Reference No: JMD/2024-25/013
    private Instant billReferenceDate; // 🟢 Now safely captured from incoming frontend payload date snapshots
    private String poNumber;           // P.O: 5045921115
    private Instant poDate;            // 🟢 Now safely captured from incoming frontend payload date snapshots
    
    // Logistics Snapshot Fields
    private String transportMode;      // Mode Of Transport: Road
    private String ewayBillNo;
    private String scnNo;

    // Client Snapshot Info
    private String clientId;
    private String clientName;
    private String billingAddress;     // BILLED TO
    private String shippingAddress;    // SHIPPED TO
    private String clientGst;          // Snapshot GSTIN field (Editable Override)
    private String clientState;        // e.g., Maharashtra
    private String clientStateCode;    // e.g., 27

    // Financial Breakdowns
    private List<CreditNoteItem> items;
    private double subtotal;           // Sum of item amounts (15,885)
    private double cgstRate;           // e.g., 9.0
    private double cgstAmount;         // 1,430
    private double sgstRate;           // e.g., 9.0
    private double sgstAmount;         // 1,430
    private double totalAmount;        // 18,744.30
    private String rupeesInWords;      // Rupees: Eighteen Thousand...

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditNoteItem {
        private String description;    // e.g., ALUMINIUM FRAMES WITH LED
        private String hsn;            // e.g., 94059900
        private String uom;            // e.g., SQFT
        private int qty;               // 30
        private double rate;           // 472
        private double amount;         // qty * rate
        private double taxPercent;     // 🟢 Added: Stores total item GST rate (18.0) for perfect React re-renders!
    }
}