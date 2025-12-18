package com.billingapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreateInvoiceRequest {

    @NotBlank(message = "clientId is required")
    private String clientId;

    @NotEmpty(message = "At least one invoice item is required")
    private List<InvoiceItemRequest> items;  // validated item list

    @PositiveOrZero(message = "Tax must be zero or positive")
    private double tax;

    // allowed values: DRAFT | PAID | UNPAID | PARTIAL
    private String status;

    private Instant issuedAt;
    private Instant dueDate;

    private String createdBy;
    // 👇 NEW FIELDS
    private String billingAddress;
    private String shippingAddress;
    private String ewayBillNo;
    private String transportMode;
    private String challanNo;
    private Instant challanDate;
    private String poNumber;
    private Instant poDate;
}
