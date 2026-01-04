package com.billingapp.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "estimates")
public class Estimate {

    @Id
    private String id;

    private String estimateNo;     // e.g. JMD/2025-26/147
    private LocalDateTime estimateDate;

    // Client Link
    private String clientId;
    private String clientName;
    private String billingAddress; // Stored snapshot
    private String gstin;
    private String attention;

    private String subject;        // e.g. "PROJECT LOCATION : NYKAA LUXE..."

    private List<EstimateItem> items = new ArrayList<>();

    // Totals
    private double subTotal;
    private double taxAmount;
    private double total;
    
    private String status; // DRAFT, SENT, APPROVED

    private String notes; // Terms & Conditions

    @CreatedDate
    private LocalDateTime createdAt;
    
    @Data
    public static class EstimateItem {
        private String description;
        private String hsnCode;
        
        // 👇 CHANGED: 'totalUnit' -> 'unit' to match Frontend Interface
        private String unit; 
        
        private double qty;
        private double rate;
        private double taxRate;   
    }
}